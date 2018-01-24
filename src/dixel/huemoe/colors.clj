(ns dixel.huemoe.colors
  (:require [clojure.math.numeric-tower :as math]))

;; formulas from [hue documentation](https://developers.meethue.com/documentation/color-conversions-rgb-xy) (needs login)

(defn rgb->xy
  [[r g b]]
  (let [rn (/ r 255.0)
        gn (/ g 255.0)
        bn (/ b 255.0)
        normalize (fn [val]
                    (if (> val 0.04045)
                      (math/expt (/ (+ val 0.055) 1.055) 2.4)
                      (/ val 12.92)))
        r (normalize rn)
        g (normalize gn)
        b (normalize bn)
        X (+ (* r 0.664511)
             (* g 0.154324)
             (* b 0.162028))
        Y (+ (* r 0.283881)
             (* g 0.668433)
             (* b 0.047685))
        Z (+ (* r 0.000088)
             (* g 0.072310)
             (* b 0.986039))
        x (/ X (+ X Y Z))
        y (/ Y (+ X Y Z))]
    [x y (math/round (* Y 255))]))

(defn xy->rgb
  [[x y] & [bri]]
  (let [z (- 1.0 x y)
        Y (or bri 1.0)
        X (* x (/ 1.0 y))
        Z (* z (/ Y y))
        rn (min 1.0 (+ (* X 1.656492)
                       (* Y -0.354851)
                       (* Z -0.255038)))
        gn (min 1.0 (+ (* X -0.707196)
                       (* Y 1.655397)
                       (* Z 0.036152)))
        bn (min 1.0 (+ (* X 0.051713)
                       (* Y -0.121364)
                       (* Z 1.011530)))
        denormalize (fn [val]
                      (if (<= val 0.0031308)
                        (* val 12.92)
                        (-
                         (* 1.055 (math/expt val (/ 1.0 2.4)))
                         0.055)))
        r (denormalize rn)
        g (denormalize gn)
        b (denormalize bn)
        round-fn #(math/round (* % 255))]
    [(round-fn r) (round-fn g) (round-fn b)]))
