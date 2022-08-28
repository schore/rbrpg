(ns lum.maputil)

(def sizex 50)
(def sizey 30)

(defn position-to-n
  [x y]
  (+ x (* y sizex)))

(defn n-to-position
  [n]
  (let [x (mod n sizex)
        y (quot n sizex)]
    [x y]))

(defn get-tile
  ([input n]
   (let [[x y] (n-to-position n)]
     (get-tile input x y)))
  ([input x y]
   (nth input (+ x (* y sizex)))))

(defn to-map
  [col]
  (map (fn [i] {:type i :visible? false}) col))

(defn to-map2
  [col]
  (for [x (range sizex)
        y (range sizey)]
    (let [wall (get-tile col x y)]
      [[x y] wall])))

(defn inmap?
  [x y]
  (and (>= x 0)
       (< x sizex)
       (>= y 0)
       (< y sizey)))
