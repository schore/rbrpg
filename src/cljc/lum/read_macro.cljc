(ns lum.read-macro
  (:require [clojure.java.io :as io]
            [markdown-to-hiccup.core :as md]))

(defmacro md-to-inline-hiccup [resource]
  (-> resource
      io/resource
      slurp
      md/md->hiccup
      md/component))

;;(println  (convert-readme-to-hiccup "docs/docs.md"))
