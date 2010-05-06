(ns com.stuartsierra.lazytest.attach
  (:use [com.stuartsierra.lazytest.groups
         :only (group?)]))

(defn groups
  "Get the set of example groups from obj's metadata."
  [obj]
  (::groups (meta obj)))

(defn set-groups!
  "Set iref's set of example groups to grps, which must be a set."
  [iref grps]
  {:pre [(set? grps)
         (every? group? grps)]}
  (alter-meta! iref assoc ::groups grps))

(defn add-group!
  "Add grp to the set of example groups on iref."
  [iref grp]
  {:pre [(group? grp)]}
  (alter-meta! iref assoc ::groups
               (conj (::groups (meta iref) #{}) grp)))

(defn remove-group!
  "Remove grp from the set of example groups on iref."
  [iref grp]
  {:pre [(group? grp)]}
  (alter-meta! iref update-in [::groups] disj grp))

(defn clear-groups!
  "Remove all example groups from iref."
  [iref]
  (alter-meta! iref dissoc ::groups))



;;; Assertions

(let [g1 (com.stuartsierra.lazytest.groups/group
          [] (com.stuartsierra.lazytest.groups/group-examples (= 1 1))
          [])
      g2 (com.stuartsierra.lazytest.groups/group
          [] (com.stuartsierra.lazytest.groups/group-examples (= 2 2))
          [])]
  (assert (nil? (groups *ns*)))

  (set-groups! *ns* #{g1 g2})
  (assert (= #{g1 g2} (groups *ns*)))

  (remove-group! *ns* g1)
  (assert (= #{g2} (groups *ns*)))

  (add-group! *ns* g1)
  (assert (= #{g1 g2} (groups *ns*)))

  (clear-groups! *ns*)
  (assert (nil? (groups *ns*))))
