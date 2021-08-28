(ns lum.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [lum.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[lum started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[lum has shut down successfully]=-"))
   :middleware wrap-dev})
