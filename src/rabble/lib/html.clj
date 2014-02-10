(ns rabble.lib.html
  (:require [me.raynes.cegdown :as md]
            [flyingmachine.webutils.utils :refer :all]))

(defn md-content
  [content]
  (let [content (or (:content content) content "")]
    (md/to-html content [:fenced-code-blocks :autolinks :suppress-all-html])))

(defn mask-deleted
  [content-fn]
  (fn [ent]
    (if (:content/deleted ent)
      "<em>deleted</em>"
      (content-fn ent))))
