(ns pradpi.url
  "Functions for handling urls."
  (:require [clojure.java.io :refer [as-url]]
            [clojure.string :refer [split replace join]])
  (:import  [java.net URLEncoder])
  (:refer-clojure :exclude [replace]))

(defn query
  "Get the query portion of the given url"
  [url]
  (->> (as-url url)
       (.getQuery)))

(defn rfc3986
  "Apply percent encoding via rfc3986 to the given string"
  [str]
  (-> (URLEncoder/encode str)
      (replace "+" "%20")
      (replace "*" "%2A")
      (replace "%7E" "~")))

(defn pairs
  "Takes a vector of 'key=value' pairs and returns a byte sorted map"
  [pairstrs]
  (->> (map #(apply hash-map (split % #"=")) pairstrs)
       (apply merge)
       (reduce-kv #(assoc %1 %2 (rfc3986 %3)) {})
       (into (sorted-map))))

(defn create
  "Create a url string from a root url string and a map"
  [root params]
  (->> (map #(str (name (first %)) "=" (second %)) params)
       (join "&")
       (str root "?")))
