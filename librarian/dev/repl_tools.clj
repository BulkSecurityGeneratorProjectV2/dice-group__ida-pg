(ns repl-tools
  (:require [proto-repl-charts.graph :as prg]
            [datascript.core :as d]
            [librarian.scraper.io.scrape :refer [create-scrape]]
            [librarian.model.io.scrape :refer [read-scrape]]
            [librarian.model.concepts.call :as call]
            [librarian.model.concepts.call-value :as call-value]
            [librarian.model.concepts.call-parameter :as call-parameter]
            [librarian.model.concepts.call-result :as call-result]
            [librarian.model.concepts.named :as named]
            [librarian.model.concepts.namespaced :as namespaced]
            [librarian.model.concepts.typed :as typed]
            [librarian.model.concepts.basetype :as basetype]
            [librarian.model.concepts.goal-type :as goal-type]
            [librarian.model.concepts.semantic-type :as semantic-type]
            [librarian.generator.query :as gq]))

(def show-scrape (comp :db read-scrape create-scrape))

(defn show-state
  [state]
  (let [db (:db state)
        nodes (d/q '[:find ?node ?type
                     :in $ %
                     :where [(ground [::call/call
                                      ::call-value/call-value
                                      ::call-parameter/call-parameter
                                      ::call-result/call-result])
                             [?type ...]]
                            (type ?node ?type)]
                   db gq/rules)
        nodes (map (fn [[node type]]
                     {:id node
                      :group type
                      :label
                      (str
                        (case type
                          ::call/call
                          (clojure.string/join "/" (get-in (d/entity db node)
                                                           [::call/callable
                                                            ::namespaced/id]
                                                           ["?" "?"]))
                          ::call-value/call-value
                          (::call-value/value (d/entity db node))
                          ::call-parameter/call-parameter
                          (get-in (d/entity db node)
                                  [::call-parameter/parameter
                                   ::named/name]
                                  "?")
                          ::call-result/call-result
                          (get-in (d/entity db node)
                                  [::call-result/result ::named/name]
                                  "?")
                          "?")
                        " (" node ")"
                        "\n<"
                        (->> (::typed/datatype (d/entity db node))
                             (map (fn [datatype]
                                    (case (first (:type datatype))
                                      ::basetype/basetype
                                      (::basetype/id datatype)
                                      ::goal-type/goal-type
                                      (str "goal:" (name (::goal-type/id datatype)))
                                      ::semantic-type/semantic-type
                                      (str "s:" (name (::semantic-type/key datatype)) ":"
                                           (name (::semantic-type/value datatype)))
                                      "?")))
                             (clojure.string/join ", "))
                        ">")})
                   nodes)
        receive-edges (->> nodes
                           (filter #(= (:group %) ::call-parameter/call-parameter))
                           (mapcat (fn [{:keys [id]}]
                                     (d/q '[:find ?source ?param
                                            :in $ ?param
                                            :where [?param ::call-parameter/receives ?source]]
                                          db id)))
                           (map (fn [[from to]] {:from from, :to to, :label "flow"})))
        parameter-edges (->> nodes
                           (filter #(= (:group %) ::call-parameter/call-parameter))
                           (mapcat (fn [{:keys [id]}]
                                     (d/q '[:find ?param ?call
                                            :in $ ?param
                                            :where [?call ::call/parameter ?param]]
                                          db id)))
                           (map (fn [[from to]] {:from from, :to to, :label "param"})))
        result-edges (->> nodes
                        (filter #(= (:group %) ::call-result/call-result))
                        (mapcat (fn [{:keys [id]}]
                                  (d/q '[:find ?call ?result
                                         :in $ ?result
                                         :where [?call ::call/result ?result]]
                                       db id)))
                        (map (fn [[from to]] {:from from, :to to, :label "result"})))
        edges (concat receive-edges
                      parameter-edges
                      result-edges)
        edges (if (empty? edges) [{:from -1 :to -2}] edges)]
    (prg/graph "Control Flow State"
               {:nodes nodes
                :edges edges}
               {:edges {:arrows "to"}
                :nodes {:shape "box"
                        :labelHighlightBold false}
                :physics {:hierarchicalRepulsion {:nodeDistance 60
                                                  :springLength 80}}
                :layout {:hierarchical {:enabled true
                                        :direction "UD"
                                        :sortMethod "directed"
                                        :levelSeparation 80}}})))
