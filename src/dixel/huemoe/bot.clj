(ns dixel.huemoe.bot
  (:require [cheshire.core :as json]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [morse.api :as api]
            [morse.polling :as polling]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [cyrus-config.core :as cfg]
            [clojure.spec.alpha :as s]
            [dixel.huemoe.hue :as hue]
            [dixel.huemoe.keyboards :refer [keyboards
                                            button->command
                                            command->button]]))

(def message-buffer-size 100)

(def elements-in-keyboard-row 4)

(def user-context (atom {}))

(cfg/def telegram-token "Telegram developer token"
  {:spec string?
   :secret true
   :required true})

(cfg/def telegram-user-whitelist "Comma separated list of telegram users allowed to use this bot"
  {:spec string?
   :required true})

(cfg/def polling-timeout "Timeout before restarting the telegram polling process"
  {:spec int?
   :required false
   :default 5})

(defn get-lamp-id-from-button [button-text]
  (when button-text
    (first (str/split button-text #":"))))

(defn keyboard-reply [keyboard text chat-id]
  (api/send-text telegram-token
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
  (let [{:keys [dimmable-light dimmable-color-light]} keyboards]
    (keyboard-reply
     (if colorful?
       dimmable-color-light
       dimmable-light)
     "`.:dimmable lamp control:.`" chat-id)))

(defn light-color-fine-control [chat-id]
  :pass)

(defn handle-device-command
  "Handcrafted set of commands for hue lamps settings based on emojis"
  [lamp-id chat-id button]
  (case (button->command button)
    :lamp-on (hue/set-brightness hue/hue lamp-id hue/max-brightness)
    :lamp-off (hue/set-brightness hue/hue lamp-id 0)
    :brightness-inc (hue/increase hue/hue lamp-id)
    :brightness-dec (hue/decrease hue/hue lamp-id)
    :brightness-1 (hue/set-brightness hue/hue lamp-id hue/min-brightness)
    :brightness-2 (hue/set-brightness hue/hue lamp-id (* 5 hue/brightness-step))
    :brightness-3 (hue/set-brightness hue/hue lamp-id (* 10 hue/brightness-step))
    :brightness-4 (hue/set-brightness hue/hue lamp-id (* 15 hue/brightness-step))
    :brightness-5 (hue/set-brightness hue/hue lamp-id (* 20 hue/brightness-step))
    :blue-heart (hue/set-light-state hue/hue lamp-id {:xy [0 0] :on true})
    :red-heart (hue/set-light-state hue/hue lamp-id {:xy [1 0] :on true})
    :green-heart (hue/set-light-state hue/hue lamp-id {:xy [0 1] :on true})
    :yellow-heart (hue/set-light-state hue/hue lamp-id {:xy [0.5 0.5] :on true})
    :purple-heart (hue/set-light-state hue/hue lamp-id {:xy [0.5 0.3] :on true})
    :flame (hue/set-light-state hue/hue lamp-id {:ct 400 :on true})
    :snowflake (hue/set-light-state hue/hue lamp-id {:ct 153 :on true})
    :droplet (hue/set-light-state hue/hue lamp-id {:ct 300 :on true})
    :gear (light-color-fine-control chat-id)
    (do
      (swap! user-context
             #(assoc-in % [chat-id :last-button] nil))
      (light-listing-panel chat-id))))

(defn handle-generic-command [chat-id command]
  (if-let [lamp-id (get-lamp-id-from-button (:last-button (@user-context chat-id)))]
    (handle-device-command lamp-id chat-id command)
    (cond
      (= command (:all-off command->button))
      (doseq [i (hue/get-active-device-ids hue/hue)]
        (hue/set-brightness hue/hue i 0))
      ((hue/get-active-device-ids hue/hue) (get-lamp-id-from-button command))
      (do
        (let [lamp-id (get-lamp-id-from-button command)]
          (swap! user-context
                 #(assoc-in % [chat-id :last-button] lamp-id))
          (light-control-panel chat-id (hue/is-color-lamp? hue/hue lamp-id))))
      :default (do (swap! user-context
                          #(assoc-in % [chat-id :last-button] nil))
                   (light-listing-panel chat-id)))))

(defn message-dispatcher [message]
  (let [chat-id (-> message :message :chat :id)
        text (-> message :message :text)
        user (-> message :message :from :username)
        user-whitelist (into #{} (str/split telegram-user-whitelist #","))]
    (when (user-whitelist user)
      (handle-generic-command chat-id text))))

(defn start-polling
  "Start telegram polling and return running/updates channels"
  []
  (let [updates (a/chan message-buffer-size)
        runner (polling/start telegram-token
                              (fn [message]
                                (a/go (a/>! updates message)))
                              {:timeout polling-timeout})]
    {:runner runner
     :updates updates}))

(defn polling-control [control-channel]
  (a/go-loop [polling (start-polling)]
    (let [{:keys [updates runner]} polling]
      (a/alt!
        updates ([r] (if-let [result r]
                       (do
                         (try
                           (log/debugf "got message: %s" result)
                           (message-dispatcher result)
                           (catch Exception e
                             (log/errorf "unable to process %s: %s" result e)))
                         (recur polling))
                       (do
                         (log/error "updates channel is closed or the result is empty; restarting polling...")
                         (polling/stop runner)
                         (recur (start-polling)))))
        runner (do
                 (log/error "runner channel is closed, restarting polling...")
                 (polling/stop runner)
                 (recur (start-polling)))
        control-channel (do
                          (log/info "got stop signal, terminating polling...")
                          (polling/stop runner))))))

(mount/defstate bot
  :start (let [control-channel (a/chan)]
           (polling-control control-channel)
           control-channel)
  :stop (a/close! bot))
