(ns pradpi.hmac
  (:require [clojure.java.io :refer [as-url]]
            [clojure.string :refer [join split]]
            [pradpi.url :as u]
            [clj-time.format :as format]
            [clj-time.core :as time])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [org.apache.commons.codec.binary Base64]))

(def ^:const algo "HmacSHA256")

(defn- raw-hmac
  "Returns a byte array of the string signed with the given key"
  [to-sign key]
  (let [bytes (.getBytes key "UTF-8")
        secret (SecretKeySpec. bytes algo)
        mac (doto (Mac/getInstance algo) (.init secret))]
    (.doFinal mac to-sign)))

(defn- now
  "Gets a UTC timestamp for the time right now"
  []
  (let [now (time/now)
        formatter (format/formatters :date-time-no-ms)]
    (format/unparse formatter now)))

(defn- canonical
  "Get the canonical string to sign for an amazon request"
  [url]
  (-> (u/query url)
      (split #"&")
      (u/pairs)
      (#(map (fn [[k v]](str k "=" v)) %))
      (#(join "&" %))))

(defn- signable
  "Returns a string that is ready for signature"
  [method url]
  (let [jurl (as-url url)]
    (join "\n" [method
                (.getHost jurl)
                (.getPath jurl)
                (canonical jurl)])))

(defn- sign
  "Signs a request with the given key"
  [method url key]
  (-> (signable method url)
      (.getBytes "UTF-8")
      (raw-hmac key)
      (#(.encode (Base64.) %))
      (String.)
      (u/rfc3986)))

(defn signed
  "Get a signed url for use in the ad API. Adds Timestamp, and Signature query params"
  ([method url key timestamp]
   (let [stamped (str url "&Timestamp=" timestamp)
        signature (sign method stamped key)]
    (str stamped "&Signature=" signature)))
  ([url key] (signed "GET" url key (now))))
