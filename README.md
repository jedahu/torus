# Torus

Torus is a Clojurescript web application library inspired by
[ring](https://github.com/mmcgrana/ring).

Like ring, there is a SPEC.md file at the root of the torus repository which
describes the vanilla request and response interfaces. More information can be
found in the docstrings of me.panzoo.{torus,middleware/interfaces}, most of
which are reproduced in more readable form in the
[wiki](https://github.com/jedahu/torus/wiki/Torus-documentation).


## Usage

Add `[me.panzoo/torus "0.0.2"]` to `:dependencies` in your Leiningen project.


## Synopsis

Clojurescript: src/your/ns.cljs

    (ns your.ns
      (:require
        [me.panzoo.torus :as torus]
        [goog.dom :as dom]))
    
    (defn handler [req]
      {:id "default handler"
       :title "Hello Torus"
       :body [(dom/createDom "h1" nil (dom/createTextNode "Hello!"))
              (dom/createDom "p" nil (dom/createTextNode "Torus!"))]})
    
    (defn ^:export run []
      (torus/init handler))

Run `"$CLOJURESCRIPT_HOME/bin/cljsc" src >out/deps.js`

HTML: index.html

    <!DOCTYPE html>
    <html>
      <head>
        <script src='out/goog/base.js'></script>
        <script src='out/goog/deps.js'></script>
        <script src='out/deps.js'></script>
        <script>
          goog.require('your.ns')
        </script>
      </head>
      <body>
        <script>
          your.ns.run()
        </script>
      </body>
    </html>

Adding middleware:

    (defn wrap-article [handler]
      (fn [req]
        (let [resp (handler req)
              body (:body resp)]
          (assoc resp :body [(apply dom/createDom "article" nil body)]))))
    
    (defn ^:export run []
      (torus/init (wrap-article handler)))
