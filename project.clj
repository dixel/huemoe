(defproject huemoe "0.0.6"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main dixel.huemoe.core
  :uberjar-name "huemoe.jar"
  :profiles {:uberjar
             {:aot :all}}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.6.3"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]
                 [morse "0.3.4"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [cyrus/config "0.2.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]])
