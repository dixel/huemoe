(defproject huemoe "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main huemoe.core
  :profiles {:uberjar
             {:aot :all}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.6.3"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]
                 [org.apache.xmlrpc/xmlrpc-client "3.1.3"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]
                 [morse "0.3.0"]
                 [org.clojure/core.async "0.3.465"]])
