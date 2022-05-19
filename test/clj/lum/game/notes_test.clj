(ns lum.game.notes-test)

(defn propability
  "Calculates probability of advangtage
  `g` number to achieve
  `n` n sided dice"
  [g n]
  (/ (* (+ n (- g) 1)
        (+ n g -1))
     (* n n)))


;;(g-1)(2n-g+1)
(defn disadvantage
  [g n]
  (- 1 (/ (* (+ g -1)
             (+ (* 2 n) (- g) 1))
          (* n n))))

(defn create-table
  [n]
  (for [i (range 1 (inc n))]
    [i (float (propability i n)) (float (disadvantage i n))]))

;;11 1: 4/4 4/4
;;12 2: 3/4 1/4
;;21
;;22
;;
;;11 1: 9/9 9/9
;;12 2: 8/9 4/9
;;13 3: 5/9 1/9
;;21
;;22
;;23
;;31
;;32
;;33
;;
;;11 1: 4*4+0*0/16   1-  0*4+4*0/16
;;12 2: 3*4+1*3/16   1-  1*4+3*1/16
;;13 3: 2*4+2*2/16   1-  2*4+2*2/16
;;14 4: 1*4+3*1/16   1-  3*4+1*3/16
;;21
;;22
;;23
;;24
;;31
;;32
;;33
;;34
;;41
;;42
;;43
;;44

;;(g-1)n+(n-(g-1))*(g-1)
