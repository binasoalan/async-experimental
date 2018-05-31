(ns binasoalan.login-test
  (:require [binasoalan.test-utils :refer [route-to with-user path-driver]]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]))

(use-fixtures :once with-user)

(deftest test-login-flow
  (with-firefox-headless {:path-driver path-driver} driver
    (testing "login page"
      (doto driver
        (go (route-to "/login"))
        (wait-visible {:tag :input :type :submit}))

      (is (not (-> driver (visible? {:fn/has-text "invalid input"}))))
      (is (not (-> driver (visible? {:fn/has-text "wrong username/password"})))))

    (testing "empty input"
      (doto driver
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "invalid input"}))))

    (testing "empty password for non-existent-user"
      (doto driver
        (fill {:tag :input :name :username} "burhan")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "invalid input"}))))

    (testing "empty username for non-existent-user"
      (doto driver
        (fill {:tag :input :name :password} "abc123")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "invalid input"}))))

    (testing "non-existent-user with valid input"
      (doto driver
        (fill {:tag :input :name :username} "burhan")
        (fill {:tag :input :name :password} "abc123")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "wrong username/password"}))))

    (testing "empty password for real user"
      (doto driver
        (fill {:tag :input :name :username} "hodor")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "invalid input"}))))

    (testing "empty username for real user"
      (doto driver
        (fill {:tag :input :name :password} "hodor123")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "invalid input"}))))

    (testing "real user with valid input"
      (doto driver
        (fill {:tag :input :name :username} "hodor")
        (fill {:tag :input :name :password} "hodor123")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :body}))

      (is (-> driver (visible? {:fn/has-text "Logged in"}))))))
