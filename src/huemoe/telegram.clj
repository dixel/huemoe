(ns huemoe.telegram
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [morse.api :as api]
            [clojure.core.async :as a]))

(defn new-offset
  "Returns new offset for Telegram updates; from https://github.com/Otann/morse/blob/master/src/morse/polling.clj"
  [updates default]
  (if (seq updates)
    (-> updates last :update_id inc)
    default))

(defn start
  "Modified polling from https://github.com/Otann/morse/blob/master/src/morse/polling.clj"
  ([token] (start token {}))
  ([token opts]
   (let [updates (a/chan)
         runner (a/chan)
         timeout (or (:timeout opts) 5000)]
     (a/go-loop [offset 0]
       (let [response (a/thread
                        (try
                          (api/get-updates token (merge opts {:offset offset}))
                          (catch Exception e
                            (log/errorf "exception while trying to get updates: %s" e)
                            ::api/error)))
             [data _] (a/alts! [response runner])]
         (case data
           :close (log/info "stopping telegram polling process...")
           nil (log/error "got unexpected nil as a response")
           ::api/error
           (do (log/warn "Got error from Telegram API, retrying in" timeout "ms")
               (a/<! (a/timeout timeout))
               (recur offset))
           (do
             (doseq [upd data] (when upd (a/>! updates upd)))
             (recur (new-offset data offset))))))
     {:updates updates
      :runner runner})))

(defn stop [{:keys [runner]} polling-state]
  (a/>!! runner :close))
