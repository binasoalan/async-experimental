(ns binasoalan.config
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.crypto :as crypto]))

(def iv (codecs/hex->bytes "dfac7da1c0676aa2693159ae5c73c86f"))
(def key (codecs/hex->bytes "b2ae2cda0a68f951323d5bbef25b93e28748e16440941985b3d275ecbe044bca"))

(defn encrypt [str]
  (-> str codecs/to-bytes (crypto/encrypt key iv) codecs/bytes->hex))

(defn decrypt [str]
  (-> str codecs/hex->bytes (crypto/decrypt key iv) codecs/bytes->str))
