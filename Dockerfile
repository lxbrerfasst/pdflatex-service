FROM fedora:34

WORKDIR /home

RUN dnf install -y \
	texlive-scheme-basic \
	latexmk \
	"tex(sourcesanspro.sty)" \
	"tex(ly1enc.def)" \
	"tex(xltabular.sty)" \
	"tex(ltablex.sty)" \
	"tex(numprint.sty)"

RUN dnf install -y java-11-openjdk-headless

RUN curl -O https://download.clojure.org/install/linux-install-1.10.1.727.sh
RUN chmod +x linux-install-1.10.1.727.sh
RUN ./linux-install-1.10.1.727.sh
RUN rm linux-install-1.10.1.727.sh

RUN mkdir "pdfservice"
ADD src pdfservice/src/
ADD deps.edn pdfservice/

WORKDIR /home/pdfservice

RUN clojure -P

ENV PORT=3070

ENTRYPOINT ["clojure", "-X", "com.github.lxbrerfasst.pdfservice/-main"]
EXPOSE 3070

