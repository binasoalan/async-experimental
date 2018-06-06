(ns binasoalan.security
  (:require [binasoalan.middlewares :refer [wrap-access-rules wrap-authentication]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [ring.util.response :refer :all]))

(def auth-backend (backends/session))

(defn user-access [request]
  (authenticated? request))

(def access-rules [{:pattern #"^/app.*"
                    :handler user-access}])

(defn unauthorized-handler [request val]
  (-> (response "Not authorized")
      (content-type "text/html")
      (status 403)))

(defn wrap-security
  [handler]
  (-> handler
      (wrap-access-rules {:rules access-rules :on-error unauthorized-handler})
      (wrap-authentication auth-backend)))
