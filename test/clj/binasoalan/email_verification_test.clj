(ns binasoalan.email-verification-test
  (:require [binasoalan.test-utils :refer [path-driver route-to
                                           with-unverified-user]]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]))

(use-fixtures :once with-unverified-user)

(deftest test-email-verification
  (with-firefox-headless {:path-driver path-driver} driver
    (testing "no input"
      (doto driver
        (go (route-to "/sahkan"))
        (wait-visible {:tag :body}))

      (is (-> driver get-url (= (route-to "/login")))))

    (testing "invalid token"
      (doto driver
        (go (route-to "/sahkan?token=abc123defghik"))
        (wait-visible {:tag :body}))

      (is (-> driver get-url (= (route-to "/login")))))

    (testing "valid token"
      (doto driver
        (go (route-to "/sahkan?token=somerandomtoken"))
        (wait-visible {:tag :body}))

      (is (-> driver get-url (= (route-to "/verified")))))))
