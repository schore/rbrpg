(ns lum.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[lum started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[lum has shut down successfully]=-"))
   :middleware identity})
