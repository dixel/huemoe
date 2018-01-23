(ns dixel.huemoe.hue
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [cyrus-config.core :as cfg]))

(def max-brightness 254)

(def min-brightness 1)

(def brightness-step 10)

(cfg/def hue-host "IP address of the Hue Bridge"
  {:spec string?
   :required true})

(cfg/def hue-token "Developer token for Hue"
  {:spec string?
   :secret true
   :required true})

(defn get-user-token [state username device]
  (let [response
        (-> (http/post (state :base-url) {:body
                                          (-> {:devicetype (str username ":" device)}
                                              (json/encode))})
            :body
            (json/decode true))]
    (case (-> response first :error :type)
      101 (log/error "press button on HUE and repeat")
      nil (-> response first :success :username))))

(defn get-lights [state]
  (-> (format "%s/%s/lights" (state :base-url) (state :token))
      http/get
      :body
      (json/decode true)))

(defn get-active-device-ids [state]
  (->> (get-lights state)
       (filter #(-> % second :state :reachable))
       (map first)
       (map name)
       (into #{})))

(defn is-color-lamp? [state lamp-id]
  (= (:type ((get-lights state) (keyword lamp-id)))
     "Extended color light"))

(defn set-light-state [state id opts]
  (-> (format "%s/%s/lights/%s/state" (state :base-url) (state :token) id)
      (http/put {:body (json/encode opts)})
      :body
      (json/decode true)))

(defn set-brightness [state id brightness]
  (set-light-state state id {:on (if (> brightness 0) true false)
                             :bri brightness}))

(defn increase [state id]
  (let [current-lamp-state (-> (get-lights state)
                               (get-in [(keyword id)
                                        :state]))]
    (set-brightness state id (+ brightness-step (:bri current-lamp-state)))))

(defn decrease [state id]
  (let [current-lamp-state (-> (get-lights state)
                               (get-in [(keyword id)
                                        :state]))]
    (set-brightness state id (- (:bri current-lamp-state) brightness-step))))

(mount/defstate hue
  :start {:token hue-token
          :base-url (format "http://%s/api" hue-host)})
