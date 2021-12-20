# omkamra.sequencer-fluidsynth

[![Clojars Project](https://img.shields.io/clojars/v/com.github.omkamra/sequencer-fluidsynth.svg)](https://clojars.org/com.github.omkamra/sequencer-fluidsynth)

An [omkamra.sequencer](https://github.com/omkamra/sequencer)
[target](https://github.com/omkamra/sequencer#targets) for the
[FluidSynth](https://www.fluidsynth.org/) SoundFont synthesizer.

## Usage

```clojure
(ns com.example.sequencer-fluidsynth
  (:require [omkamra.sequencer :as sequencer])
  (:require [omkamra.sequencer.targets.fluidsynth]))

(def fluid-target
  (sequencer/make-target [:fluidsynth "/usr/share/soundfonts/FluidR3_GM.sf2"]))

(sequencer/register-target fluid-target)

(def s (sequencer/create))

(def pattern
  [:bind {:target fluid-target
          :channel 0
          :velocity 100
          :root :f#3
          :scale :minor
          :dur 3}
   [:program 0]
   (for [i (range 8)]
     [:degree i])])

(sequencer/play s pattern)
```

This target supports all features provided by the
[midi](https://github.com/omkamra/sequencer-midi) target.

## License

Copyright © 2021 Balázs Ruzsa

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
