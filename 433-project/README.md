
# Team member
Inhyuk Na

# Milestones
* ~2021.11.24: development environment setup
* ~2021.11.26: coding
* ~2021.11.28: debugging

# How to run
1. Run `sbt docker:publishLocal` in `scala/`
	- This makes docker \<image>:\<tag> as cs434project:0.1

2. Run `docker-compose up` in project root
	- This runs docker app `master`, `slave_xxx` by using the image built at step 1. 
