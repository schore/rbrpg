(ns lum.events
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   ))

;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
  :page/init-home
  (fn [_ _]
    {:dispatch [:fetch-docs]}))

(rf/reg-event-fx
 :initialize
 (fn [_ _]
   {:dispatch-n (list [:test/initialize-plus]
                      [:game/initialize]
                      [:fetch-docs])}))

(rf/reg-event-db
 :test/initialize-plus
 (fn [db []]
   (assoc db :test/count 1)))



(rf/reg-event-fx
 :test/plus
 (fn [cofx _]
   (let [[_ a b] (:event cofx)]
     {:http-xhrio {:method :get
                   :uri (str "/plus/" a "/" b)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:test/plus-response]
                   }})))

(rf/reg-event-db
 :test/plus-response
 (fn [db [_ resp]]
   (assoc db :test/count (:result resp))))


;; (GET (str "/plus/" @state "/" @state)
;;   {:handler (fn [resp]
;;               (reset! state (:result resp)))})

;;subscriptions

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-sub
 :test/count
 (fn [db _]
   (:test/count db)))
