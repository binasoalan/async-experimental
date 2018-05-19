(ns binasoalan.validation
  (:require [struct.core :as s]))

(def registration-schema
  {:username [s/required s/string]
   :email [s/required s/email]
   :password [s/required s/string]})

(def login-schema
  {:username [s/required s/string]
   :password [s/required s/string]})

(defn validate-registration [req]
  (s/validate req registration-schema))

(defn validate-login [req]
  (s/validate req login-schema))
