build-TripProcessingFunction:
	./mvnw -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp -R target/classes/. $(ARTIFACTS_DIR)/
	cp -R target/dependency/. $(ARTIFACTS_DIR)/lib/
