(ns huemoe.bot
  (:require [cheshire.core :as json]
            [clojure.core.async :as a]
            [clojure.set :refer [map-invert]]
            [clojure.string :as str]
            [huemoe.config :refer [config]]
            [huemoe.hue :as hue]
            [huemoe.telegram :as telegram]
            [morse.api :as api]
            [morse.polling :as polling]
            [mount.core :as mount]
            [taoensso.timbre :as log]))

(def message-buffer-size 100)

(def default-polling-timeout 20000)

(def elements-in-keyboard-row 4)

(def user-context (atom {}))

(def button->command
  {"🌅 on" :lamp-on
   "🌃 off" :lamp-off
   "🌃 all off" :all-off
   "🔅" :brightness-dec
   "🔆" :brightness-inc
   "1" :brightness-1
   "2" :brightness-2
   "3" :brightness-3
   "4" :brightness-4
   "5" :brightness-5
   "🔙" :back-menu
   "❌" :lamp-active
   "💡" :lamp-inactive
   "❤️" :red-heart
   "💛" :yellow-heart
   "💜" :purple-heart
   "💙" :blue-heart
   "💚" :green-heart
   "🔥" :flame
   "💧" :droplet
   "❄️" :snowflake})

(def command->button
  (map-invert button->command))

(defn get-lamp-id-from-button [button-text]
  (when button-text
    (first (str/split button-text #":"))))

(defn keyboard-reply [keyboard text chat-id]
  (api/send-text (config :telegram-token)
                 chat-id
                 {:parse_mode "Markdown"
                  :reply_markup (json/encode {:keyboard keyboard})} text))

(defn light-listing-panel [chat-id]
  (let [devices-list (hue/get-lights hue/hue)
        {:keys [lamp-active lamp-inactive all-off]} command->button]
    (keyboard-reply
     (concat
      (partition-all
       elements-in-keyboard-row
       (map (fn [[lamp-id lamp-description]]
              (format "%s: %s - %s"
                      (name lamp-id)
                      (:name lamp-description)
                      (if (not (-> lamp-description
                                   :state
                                   :reachable))
                        lamp-active
                        lamp-inactive)))
            devices-list)) [[all-off]])
     "`.:hue devices:.`" chat-id)))

(defn light-control-panel [chat-id colorful?]
  (let [{:keys [lamp-on lamp-off brightness-dec brightness-inc
                brightness-1 brightness-2 brightness-3 brightness-4 brightness-5
                back-menu red-heart green-heart blue-heart yellow-heart purple-heart
                flame snowflake all-off droplet]} command->button
        keyboard [[lamp-on lamp-off]
                  [brightness-dec brightness-1
                   brightness-2 brightness-3
                   brightness-4 brightness-5 brightness-inc]]]
    (keyboard-reply
     (let [kb
           (if colorful?
             (-> keyboard
                 (conj [snowflake droplet flame])
                 (conj [red-heart yellow-heart green-heart
                        blue-heart purple-heart]))
             keyboard)]
       (conj kb [back-menu]))
     "`.:dimmed lamp control:.`" chat-id)))

(defn handle-device-command [lamp-id chat-id button]
  (case (button->command button)
    :lamp-on (hue/set-light-state hue/hue lamp-id true hue/max-brightness)
    :lamp-off (hue/set-light-state hue/hue lamp-id false hue/min-brightness)
    :brightness-inc (hue/increase hue/hue lamp-id)
    :brightness-dec (hue/decrease hue/hue lamp-id)
    :brightness-1 (hue/set-light-state hue/hue lamp-id true hue/min-brightness)
    :brightness-2 (hue/set-light-state hue/hue lamp-id true (* 5 hue/brightness-step))
    :brightness-3 (hue/set-light-state hue/hue lamp-id true (* 10 hue/brightness-step))
    :brightness-4 (hue/set-light-state hue/hue lamp-id true (* 15 hue/brightness-step))
    :brightness-5 (hue/set-light-state hue/hue lamp-id true (* 20 hue/brightness-step))
    :blue-heart (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :xy [0 0]})
    :red-heart (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :xy [1 0]})
    :green-heart (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :xy [0 1]})
    :yellow-heart (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :xy [0.5 0.5]})
    :purple-heart (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :xy [0.5 0.3]})
    :flame (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :ct 400})
    :snowflake (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :ct 153})
    :droplet (hue/set-light-state hue/hue lamp-id true 254 {:sat 254 :ct 300})
    (do
      (swap! user-context
             #(assoc-in % [chat-id :last-button] nil))
      (light-listing-panel chat-id))))

(defn handle-generic-command [chat-id command]
  (if-let [lamp-id (get-lamp-id-from-button (:last-button (@user-context chat-id)))]
    (handle-device-command lamp-id chat-id command)
    (cond
      (= command (:all-off command->button)) (doseq
                                                 [i (hue/get-active-device-ids)]
                                               (hue/set-light-state hue/hue i false 1))
      ((hue/get-active-device-ids) (get-lamp-id-from-button command))
      (do
        (let [lamp-id (get-lamp-id-from-button command)]
          (swap! user-context
                 #(assoc-in % [chat-id :last-button] lamp-id))
          (light-control-panel chat-id (hue/is-color-lamp? lamp-id))))
      :default (do (swap! user-context
                          #(assoc-in % [chat-id :last-button] nil))
                   (light-listing-panel chat-id)))))

(defn message-dispatcher [message]
  (let [chat-id (-> message :message :chat :id)
        text (-> message :message :text)
        user (-> message :message :from :username)]
    (when ((config :telegram-user-whitelist) user)
      (handle-generic-command chat-id text))))

(defn start-polling
  "Start telegram polling and return running/updates channels"
  []
  (let [updates (a/chan message-buffer-size)
        runner (polling/start (config :telegram-token)
                               (fn [message]
                                 (a/go (a/>! updates message))))]
    {:runner runner
     :updates updates}))

(defn polling-control [control-channel]
  (let [{:keys [updates runner] :as initial-polling} (start-polling)
        timeout (config :polling-timeout default-polling-timeout)]
    (a/go-loop [polling initial-polling
                wait (a/timeout timeout)]
      (a/alt!
        updates ([r] (if-let [result r]
                       (do
                         (try
                           (log/debugf "got message: %s" result)
                           (message-dispatcher result)
                           (catch Exception e
                             (log/errorf "unable to process %s: %s" result e)))
                         (a/close! wait)
                         (recur polling (a/timeout timeout)))
                       (do
                         (log/errorf "updates channel is closed or the result is empty; restarting polling...")
                         (polling/stop runner)
                         (a/close! wait)
                         (recur (start-polling) (a/timeout timeout)))))
        runner (do
                 (log/errorf "runner channel is closed, restarting polling...")
                 (polling/stop runner)
                 (recur (start-polling) (a/timeout timeout)))
        wait (do
               (log/errorf "reached global timeout getting updates from telegram API, restarting polling process...")
               (polling/stop runner)
               (recur (start-polling) (a/timeout timeout)))
        control-channel (do
                          (log/errorf "got stop signal, terminating polling...")
                          (a/close! wait)
                          (polling/stop runner))))))

(mount/defstate bot
  :start (let [control-channel (a/chan)]
           (polling-control-fn control-channel)
           control-channel)
  :stop (a/close! bot))
