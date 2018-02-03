(ns dixel.huemoe.bot-test
  (:require [dixel.huemoe.bot :as sut]
            [dixel.huemoe.keyboards :as kbd]
            [clojure.core.async :refer [>! <! <!! >!! chan go go-loop] :as a]
            [clojure.test :refer [deftest is testing] :as t]
            [taoensso.timbre :as log]
            [cyrus-config.core :as cfg]))

(deftest polling-test
  (let [control-channel (chan)
        update [{:message 1}]
        updates (chan)
        runner (chan)
        result-channel (chan)
        polling-monitor-channel (chan)
        start-polling (fn []
                        (go (>! polling-monitor-channel "triggered"))
                        {:updates updates
                         :runner runner})
        message-dispatcher (fn [result]
                             (go (>! result-channel result)))]
    (sut/polling-control control-channel start-polling message-dispatcher)

    (testing "testing normal polling behaviour with updates from the API"
      (>!! updates update)
      (is (= update (<!! result-channel)))
      (>!! updates update)
      (is (= update (<!! result-channel))))

    (testing "when the control channel is closed - the polling process stops"
      (a/close! control-channel)
      (is (nil? (<!! runner))))))

(deftest permissions-test
  (cfg/reload-with-override! {"TELEGRAM_USER_WHITELIST" "root,admin"})

  (let [text-atom (atom {})]
    (testing "allowed users should be able to perform commands with bot"
      (let [message {:message
                     {:text "I am root"
                      :chat {:id 0}
                      :from {:username "root"}}}]
        (is (= message (sut/wrap-user-whitelist message)))))

    (testing "users not in whitelist shouldn't get handled"
      (let [message {:message
                     {:text "I am root"
                      :chat {:id 1}
                      :from {:username "but-not-really"}}}]
        (is (= nil (sut/wrap-user-whitelist message)))))))

(deftest all-lights-off-test
  (let [lamps #{"1" "2" "3"}
        lamps-state (atom {"1" 100
                           "2" 98
                           "3" 56})]
    (with-redefs
      [dixel.huemoe.hue/get-active-device-ids (fn [_] lamps)
       dixel.huemoe.hue/set-brightness (fn [_ lamp bri]
                                         (swap! lamps-state
                                                (fn [ls]
                                                  (assoc ls lamp 0))))]
      (sut/wrap-hue-action {:message {:text (kbd/command->button :all-off)}})
      (doseq [[id bri] @lamps-state]
        (is (= 0 bri))))))
