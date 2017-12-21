(ns huemoe.telegram
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [clojure.core.async :as a]))

(def base-url "https://api.telegram.org/bot")

(defn new-offset
  "Returns new offset for Telegram updates"
  [updates default]
  (if (seq updates)
    (-> updates last :update_id inc)
    default))

(defn get-updates
  "Receive updates from Bot via long-polling endpoint"
  [token {:keys [limit offset timeout]}]
  (let [url      (str base-url token "/getUpdates")
        query    {:timeout (or timeout 1)
                  :offset  (or offset 0)
                  :limit   (or limit 100)}]
    (try
      (or (->
          (http/get url {:as :json
                         :query-params query})
          :body
          :result)
          ::error)
      (catch Exception e
        (do
          (log/errorf "Failed to get updates: %s" e)
          ::error)))))

(defn send-text
  "Sends message to the chat"
  ([token chat-id text] (send-text token chat-id {} text))
  ([token chat-id options text]
   (let [url  (str base-url token "/sendMessage")
         body (into {:chat_id chat-id :text text} options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  body})]
     (-> resp :body))))

(defn start
  [token opts]
  (let [updates (a/chan)
        timeout (or (:timeout opts) 1000)]
    (a/go-loop [offset 0]
      (let [response (a/thread (get-updates token (merge opts {:offset offset})))
            data (a/<! response)]
        (case data
          ::error
          (do (log/warn "Got error from Telegram API, retrying in" timeout "ms")
              (a/<! (a/timeout timeout))
              (recur offset))
          (do
            (doseq [upd data] (when upd (a/>! updates upd)))
            (recur (new-offset data offset))))))
    updates))
