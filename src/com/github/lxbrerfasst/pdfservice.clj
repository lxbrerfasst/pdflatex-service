(ns com.github.lxbrerfasst.pdfservice
  (:require [org.httpkit.server :as http]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import (java.util.zip ZipInputStream ZipEntry)
           (java.nio.file Files Path)
           (java.nio.file.attribute FileAttribute)
           (java.util UUID)
           (java.io PipedInputStream PipedOutputStream)))

(defn unzip-stream!
  [stream ^Path target-dir]
  (with-open [zis (ZipInputStream. stream)]
    (loop []
      (when-some [entry (.getNextEntry zis)]
        (let [path (.resolve target-dir (.getName entry))
              file (.toFile path)]
          (if (.isDirectory file)
            (recur)
            (do (some-> (.getParentFile file)
                        (.mkdirs))
                (io/copy zis file)
                (recur))))))))

(defn handler
  [req]
  (let [filename (str (UUID/randomUUID))
        tmp-dir-path (Files/createTempDirectory filename (make-array FileAttribute 0))]
    (unzip-stream! (:body req) tmp-dir-path)
    (let [{:keys [exit err out]}
          (sh/with-sh-dir (str tmp-dir-path)
            (sh/sh "latexmk" "-pdf" "report.tex"))]
      (when-not (zero? exit)
        (println err))
      (let [os (PipedOutputStream.)
            is (PipedInputStream. os)]
        (future
          (with-open [os os]
            (-> (.resolve tmp-dir-path "report.pdf")
                (.toFile)
                (io/copy os)))
          (->> (file-seq (.toFile tmp-dir-path))
               (reverse)
               (run! #(io/delete-file % true))))
        {:status 200
         :body is}))))

(defn -main
  [& args]
  (let [port (or (some-> (System/getenv "PORT") (Integer/parseInt))
                 3070)]
    (http/run-server #'handler {:port port})))

(comment
  (def server (http/run-server #'handler {:port 3071}))
  (server)
  )
