# Makefile for building and running Rooster on connected devices

APP_ID := com.rooster.rooster/.MainActivity

# Device IDs
SAMSUNG_ID := 616ecbcf
PIXEL4A_ID := 0B201JECB13875
PIXEL9A_ID := 59101JEBF02652

# Default target
.PHONY: all
all: help

# === Android build & run ===

.PHONY: install
install:
	./gradlew installDebug

.PHONY: run-pixel4a
run-pixel4a: install
	adb -s $(PIXEL4A_ID) shell am start -n $(APP_ID)

.PHONY: run-pixel9a
run-pixel9a: install
	adb -s $(PIXEL9A_ID) shell am start -n $(APP_ID)

.PHONY: run-samsung
run-samsung: install
	adb -s $(SAMSUNG_ID) shell am start -n $(APP_ID)

.PHONY: devices
devices:
	adb devices

.PHONY: logs
logs:
	adb logcat --pid=$$(adb shell pidof com.rooster.rooster)

.PHONY: clean
clean:
	./gradlew clean

.PHONY: build
build:
	./gradlew build

.PHONY: test
test:
	./gradlew test

.PHONY: lint
lint:
	./gradlew lint

.PHONY: uninstall
uninstall:
	adb uninstall com.rooster.rooster

clear-data:
	adb shell pm clear com.rooster.rooster

reinstall: uninstall install

# === Help ===

.PHONY: help
help:
	@echo "Available commands:"
	@echo ""
	@echo "Build & Run:"
	@echo "  make install             - Build and install the debug APK"
	@echo "  make run-pixel4a         - Install and run on Pixel 4a (ID: $(PIXEL4A_ID))"
	@echo "  make run-pixel9a         - Install and run on Pixel 9a (ID: $(PIXEL9A_ID))"
	@echo "  make run-samsung         - Install and run on Samsung (ID: $(SAMSUNG_ID))"
	@echo ""
	@echo "Development:"
	@echo "  make build               - Build the project"
	@echo "  make clean               - Clean build artifacts"
	@echo "  make test                - Run unit tests"
	@echo "  make lint                - Run lint checks"
	@echo ""
	@echo "Device Management:"
	@echo "  make devices             - List connected ADB devices"
	@echo "  make logs                - Show logs for the running app"
	@echo "  make uninstall           - Uninstall the app from device"
	@echo "  make clear-data          - Clear app data without uninstalling"
	@echo "  make reinstall           - Uninstall and reinstall the app"

