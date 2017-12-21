(ns huemoe.bot
  (:require [mount.core :as mount]
            [huemoe.config :refer [config]]
            [huemoe.hue :as hue]
            [huemoe.telegram :as telegram]
            [clojure.core.async :as a]
            [morse.api :as api]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(def message-buffer (a/chan 100))

(def elements-in-keyboard-row 4)

(def user-context (atom {}))

(def known-lamp-list (atom nil))

(defn get-lamp-id-from-button [button]
  (when button
    (first (str/split button #":"))))

(defn get-active-device-ids []
  (->> (hue/get-lights hue/hue)
       (filter #(-> % second :state :reachable))
       (map first)
       (map name)
       (into #{})))

(defn keyboard-reply [keyboard text chat-id]
  (telegram/send-text (config :telegram-token)
                 chat-id
                 {:reply_markup (json/encode {:keyboard keyboard})} text))

(defn light-listing-panel [chat-id]
  (let [devices-list (hue/get-lights hue/hue)]
    (keyboard-reply
     (partition-all
      elements-in-keyboard-row
      (map (fn [[lamp-id lamp-description]]
             (format "%s: %s%s"
                     (name lamp-id)
                     (:name lamp-description)
                     (if (not (-> lamp-description
                                  :state
                                  :reachable))
                       " - ❌"
                       " - 💡")))
           devices-list))
     "ok" chat-id)))

(defn light-control-panel [chat-id]
  (keyboard-reply
   [["🌅 on" "🌃 off"]
    ["🔅" "1" "2" "3" "4" "5" "🔆"]
    ["🔙"]] "ok" chat-id))

(defn start-polling [token]
  (telegram/start token {}))

(defn handle-device-command [lamp-id chat-id command]
  (case command
    "🌅 on" (hue/set-light-state hue/hue lamp-id true hue/max-brightness)
    "🌃 off" (hue/set-light-state hue/hue lamp-id false hue/min-brightness)
    "🔆" (hue/increase hue/hue lamp-id)
    "🔅" (hue/decrease hue/hue lamp-id)
    "1" (hue/set-light-state hue/hue lamp-id true hue/min-brightness)
    "2" (hue/set-light-state hue/hue lamp-id true (* 5 hue/brightness-step))
    "3" (hue/set-light-state hue/hue lamp-id true (* 10 hue/brightness-step))
    "4" (hue/set-light-state hue/hue lamp-id true (* 15 hue/brightness-step))
    "5" (hue/set-light-state hue/hue lamp-id true (* 20 hue/brightness-step))
    (do
      (swap! user-context
             #(assoc-in % [chat-id :last-button] nil))
      (light-listing-panel chat-id))))

(defn handle-generic-command [chat-id command]
  (log/debugf "got command from user: %s" command)
  (if-let [lamp-id (get-lamp-id-from-button (:last-button (@user-context chat-id)))]
    (handle-device-command lamp-id chat-id command)
    (cond
      (= command "/start") (light-listing-panel chat-id)
      ((get-active-device-ids) (get-lamp-id-from-button command))
      (do
        (swap! user-context
               #(assoc-in % [chat-id :last-button] (get-lamp-id-from-button command)))
        (light-control-panel chat-id))
      :default (do (log/debugf "unknown command: %s" command)
                   (swap! user-context
                          #(assoc-in % [chat-id :last-button] nil))
                   (light-listing-panel chat-id)))))

(defn message-dispatcher [message]
  (let [chat-id (-> message :message :chat :id)
        text (-> message :message :text)
        user (-> message :message :from :username)]
    (log/debugf "got message: %s" message)
    (when ((config :telegram-user-whitelist) user)
      (handle-generic-command chat-id text))))


(mount/defstate bot
  :start (let [updates (start-polling (config :telegram-token))]
           (a/go-loop []
             (if-let [message (a/<! updates)]
               (try
                 (log/infof "message: %s" message)
                 (message-dispatcher message)
                 (catch Exception e
                   (log/errorf "unable to process %s: %s" message e))))
             (recur))
           updates)
  :stop (a/close! bot))
