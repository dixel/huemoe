(ns dixel.huemoe.keyboards
  (:require [clojure.set :refer [map-invert]]))

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

(def keyboards
  (let [{:keys [lamp-on lamp-off
                brightness-dec brightness-inc
                brightness-1 brightness-2
                brightness-3 brightness-4
                brightness-5
                back-menu
                red-heart green-heart
                blue-heart yellow-heart
                purple-heart
                flame snowflake
                droplet
                all-off]} command->button]
    {:dimmable-light [[lamp-on lamp-off]
                      [brightness-dec brightness-1
                       brightness-2 brightness-3
                       brightness-4 brightness-5
                       brightness-inc]
                      [back-menu]]
     :dimmable-color-light [[lamp-on lamp-off]
                            [brightness-dec brightness-1
                             brightness-2 brightness-3
                             brightness-4 brightness-5 brightness-inc]
                            [snowflake droplet flame]
                            [red-heart yellow-heart green-heart
                             blue-heart purple-heart]
                            [back-menu]]}))
