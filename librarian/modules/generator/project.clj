(defproject librarian/generator "1.0.0-SNAPSHOT"
  :description "Writes code to fulfill given requirements using library scrapes."

  :plugins [[lein-modules "0.3.11"]]
  :middleware [lein-modules.plugin/middleware]

  :dependencies [[org.clojure/clojure]
                 [org.clojure/tools.logging]
                 [datascript]
                 [org.clojure/data.priority-map "0.0.10"]
                 [clucie "0.4.2"]
                 [librarian/helpers]
                 [librarian/model]]

  :modules {:parent "../.."})
