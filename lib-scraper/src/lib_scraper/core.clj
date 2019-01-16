(ns lib-scraper.core
  (:require [lib-scraper.io.core :as io]
            [lib-scraper.io.config :as config]
            [lib-scraper.io.scrape :as scrape]
            [lib-scraper.model.core :as m]
            [clojure.pprint :as pp]
            [say-cheez.core :refer [capture-build-env-to]]
            [cli-matic.core :refer [run-cmd run-cmd*]])
  (:gen-class))

(capture-build-env-to BUILD)
(def PROJECT (:project BUILD))

(def ^:private ecosystems-string (clojure.string/join ", " (map name (keys m/ecosystems))))

(defn- mode-print
  [val mode]
  (case mode
    "pretty" (pp/pprint val)
    "edn" (prn val)
    (throw (Error. (str "Unknown mode " mode ".")))))

(defn scrape
  [{:keys [config out]}]
  (io/create-scrape config out))

(defn print-scrape
  [{:keys [scrape mode]}]
  (mode-print (scrape/read-scrape scrape) mode))

(defn print-config
  [{:keys [config mode]}]
  (mode-print (config/read-config config) mode))

(defn query
  [{:keys [scrape query mode]}]
  (mode-print (io/query-file scrape query) mode))

(defn print-schema
  [{:keys [ecosystem]}]
  (if-let [{:keys [concept-aliases attribute-aliases attributes version]} (m/ecosystems ecosystem)]
    (do
      (println (str "Current " (name ecosystem) " schema version: " version))
      (println "\nAttributes:")
      (pp/print-table ["attribute" "description"]
                      (->> attribute-aliases
                           (sort-by (comp #(str (namespace %) "/" (name %)) first))
                           (map (fn [[alias ident]]
                                  {"attribute" alias
                                   "description" (-> ident attributes :db/doc)}))))
      (println "\nConcept types (possible values of the :type attribute):")
      (pp/pprint (keys concept-aliases)))
    (throw (Exception. (str "Unknown ecosystem '" ecosystem "'. "
                            "Supported ecosystems: " ecosystems-string)))))

(def descs {:config "Path to a config file or to a directory containing a config named 'scraper.clj'."
            :scrape "Path to a scrape file or to a directory containing a scrape named 'scrape.db'."
            :mode "Printing mode: pretty or edn."
            :ecosystem (str "The name of a supported ecosystem. "
                            "Supported ecosystems: " ecosystems-string)})

(def CONFIGURATION
  {:app {:command (:project PROJECT)
         :description "Crawls through the documentation of software libraries and transforms them into a standardized format."
         :version (str (:version PROJECT) " (" (:git-build PROJECT) ")")}
   :commands [{:command "scrape" :short "s"
               :runs scrape
               :description "Scrapes a library using a scrape configuration and saves it."
               :opts [{:option "config" :as (:config descs) :short 0
                       :type :string, :default :present}
                      {:option "out" :short 1
                       :as (str "Optional scrape output path. "
                                "If not given, the scrape will be written into the config directory. "
                                "If a directory path is given, the scrape will be named 'scrape.db'.")
                       :type :string}]}
              {:command "print-scrape" :short "ps"
               :runs print-scrape
               :description "Prints a scrape to stdout."
               :opts [{:option "scrape" :as (:scrape descs) :short 0
                       :type :string, :default :present}
                      {:option "mode" :as (:mode descs) :short "m"
                       :type :string, :default "edn"}]}
              {:command "print-config" :short "pc"
               :description "Prints a config to stdout."
               :opts [{:option "config" :as (:config descs) :short 0
                       :type :string, :default :present}
                      {:option "mode" :as (:mode descs) :short "m"
                       :type :string, :default "pretty"}]
               :runs print-config}
              {:command "query" :short "q"
               :runs query
               :description "Performs a Datalog query on a given scrape file."
               :opts [{:option "scrape" :as (:scrape descs) :short 0
                       :type :string}
                      {:option "query" :short 1
                       :as "A Datalog query. See https://docs.datomic.com/on-prem/query.html."
                       :type :edn}
                      {:option "mode" :as (:mode descs) :short "m"
                       :type :string, :default "pretty"}]}
              {:command "print-schema" :short "psm"
               :runs print-schema
               :description "Prints the database schema of a given ecosystem."
               :opts [{:option "ecosystem" :as (:ecosystem descs) :short 0
                       :type :keyword}]}]})


(defn main*
  "Like -main but does not terminate. For runtime use only."
  [& args]
  (run-cmd* CONFIGURATION args))

(defn -main
  [& args]
  (run-cmd args CONFIGURATION))