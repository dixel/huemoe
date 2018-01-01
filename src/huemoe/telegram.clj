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
                          (or
                           (api/get-updates token (merge opts {:offset offset}))
                           :empty)
                          (catch Exception e
                            (log/errorf "exception while trying to get updates: %s" e)
                            ::api/error)))
             wait (a/timeout timeout)
             [data _] (a/alts! [response runner wait])]
         (case data
           nil (do (a/close! response) ; signaling we don't need resources there
                   (log/error "timed out waiting for get-updates")
                   (a/<! (a/timeout timeout))
                   (recur offset))
           :empty (do (log/error "got unexpected empty get-updates response")
                      (a/<! (a/timeout timeout))
                      (recur offset))
           :close (log/info "stopping telegram polling process...")
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
