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

(defn set-light-state [state id light-state brightness & opts]
  (-> (format "%s/%s/lights/%s/state" (state :base-url) (state :token) id)
      (http/put {:body (json/encode (merge (into {} opts)
                                           {:on light-state
                                            :bri brightness}))})
      :body
      (json/decode true)))

(defn increase [state id]
  (let [current-lamp-state (-> (get-lights state)
                               (get-in [(keyword id)
                                        :state]))]
    (set-light-state state id true
                     (let [new-bri (+ brightness-step (:bri current-lamp-state))]
                       (if (> new-bri max-brightness)
                         max-brightness
                         new-bri)))))

(defn decrease [state id]
  (let [current-lamp-state (-> (get-lights state)
                               (get-in [(keyword id)
                                        :state]))
        new-bri (- (:bri current-lamp-state) brightness-step)]
    (set-light-state state id (> new-bri 1)
                     (let [new-bri (- (:bri current-lamp-state) brightness-step)]
                       (if (< new-bri min-brightness)
                         min-brightness

                         new-bri)))))

(mount/defstate hue
  :start {:token hue-token
          :base-url (format "http://%s/api" hue-host)})
