(ns org.nfrac.comportex.excitation-breakdowns-test
  (:require [org.nfrac.comportex.core :as cx]
            [org.nfrac.comportex.layer :as layer]
            [org.nfrac.comportex.layer.tools :as layertools]
            [org.nfrac.comportex.encoders :as enc]
            [org.nfrac.comportex.util :as util]
            [clojure.test :as t
             :refer (is deftest testing run-tests)]))

(def bit-width 200)
(def n-on-bits 20)

(def inputs
  [:a :b :c :d
   :a :b :f :g
   :a :b :b :a
   :h :g :f :e
   :d :e :a :d])

(def initial-input {:index 0})

(defn input-transform
  [m]
  (update m :index #(mod (inc %) (count inputs))))

(defn world-seq
  "Returns a sequence of sensory input values."
  []
  (->> (iterate input-transform initial-input)
       (map #(assoc % :value (get inputs %)))))

(def sensor
  [:value
   (enc/unique-encoder [bit-width] n-on-bits)])

(def params
  {})

(defn build
  []
  (cx/network {:layer-a (layer/layer-of-cells params)}
              {:input sensor}))

(deftest exc-bd-test
  (let [[warmups continued] (split-at 50 (world-seq))
        prev-htm (reduce cx/htm-step (build) warmups)
        htm (cx/htm-step prev-htm (first continued))]
    (testing "Cell excitation breakdowns"
      (let [lyr (get-in htm [:layers :layer-a])
            wc (:winner-cells (cx/layer-state lyr))
            bd (layertools/cell-excitation-breakdowns htm prev-htm :layer-a
                                                      (conj wc [0 0]))]
        (is (every? (comp pos? :total) (map bd wc))
            "All total excitation in range.")
        (is (every? (comp pos? first vals :proximal-unstable) (map bd wc))
            "Some proximal excitation on each active column")
        (is (every? (comp map? :distal) (vals bd))
            "Distal keys hold maps.")))))
