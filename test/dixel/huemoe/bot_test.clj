(ns dixel.huemoe.bot-test
  (:require [dixel.huemoe.bot :as sut]
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
    (with-redefs [sut/handle-generic-command
                  (fn [chat-id text]
                    (swap! text-atom #(assoc % chat-id text)))]

      (testing "allowed users should be able to perform commands with bot"
        (sut/message-dispatcher {:message
                                 {:text "I am root"
                                  :chat {:id 0}
                                  :from {:username "root"}}})
        (is (= "I am root" (get @text-atom 0))))

      (testing "users not in whitelist shouldn't get handled"
        (sut/message-dispatcher {:message
                                 {:text "I am root"
                                  :chat {:id 1}
                                  :from {:username "but-not-really"}}})
        (is (= nil (get @text-atom 1)))))))
