(ns lum.core
  (:require
   [clojure.string :as string]
   [day8.re-frame.http-fx]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [lum.ajax :as ajax]
   [lum.events]
   [lum.game-main :refer [game item]]
   [markdown.core :refer [md->html]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe])
  (:import
   (goog History)))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "lum"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/item" "Items" :item]
       ;;[nav-link "#/about" "About" :about]
       ;;[nav-link "#/test" "Test" :test]
       ;;[nav-link "#/game" "Game" :game]
       ]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn test-page []
  (let [state (rf/subscribe [:test/count])]
    (fn []
      [:section.section>div.container>div.content
       [:h2 "Dann schreib ich noch was intelligentes!!"]
       [:p @state]
       [:input {:type "Button"
                :defaultValue "Click Me"
                :on-click (fn []
                            (rf/dispatch [:test/plus @state @state]))}]])))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'game
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ;; ["/about" {:name :about
    ;;            :view #'about-page}]
    ;; ["/test" {:name :test
    ;;           :view #'test-page}]
    ["/item" {:name :item
              :view #'item}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch [:initialize])
  (mount-components))
