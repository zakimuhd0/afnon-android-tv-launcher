#!/usr/bin/env bash
# Puts the TV back to stock: re-enables the Google TV launcher, any packages listed in state/disabled.log,
# gives HOME, the screensaver and accessibility slot back, puts the sleep timer back if the radio left it off,
# and uninstalls Afnon.
# Set STOCK_HOME / STOCK_DREAM if your TV's stock launcher or screensaver isn't Google TV's.

. "$(dirname "$0")/env.sh"

STOCK_HOME="${STOCK_HOME:-com.google.android.apps.tv.launcher/.home.HomeActivity}"
STOCK_DREAM="${STOCK_DREAM:-com.google.android.apps.tv.dreamx/.service.Backdrop}"
LOG="$ROOT/state/disabled.log"

# Re-enable the default Google TV launcher that was disabled during installation
echo "Re-enabling Google TV default launcher..."
adb shell pm enable --user 0 com.google.android.apps.tv.launcher 2>/dev/null || true

# Re-enable any other packages saved in the disabled log
if [ -f "$LOG" ]; then
  while read -r p; do
    [ -z "$p" ] || [ "$p" = tv.afnon.home ] && continue
    echo "$p: $(adb shell pm enable --user 0 "$p" 2>&1 | tr -d '\r')"
  done < "$LOG"
fi

# Restore stock home and screensaver components
adb shell cmd role remove-role-holder --user 0 android.app.role.HOME tv.afnon.home 2>/dev/null || true
adb shell cmd package set-home-activity "$STOCK_HOME" 2>/dev/null || true
adb shell settings put secure screensaver_components "$STOCK_DREAM"
adb shell settings delete secure enabled_accessibility_services

# Remove Afnon's remote button mappings (harmless where they were never set)
for code in $(adb shell settings get global CustomKeyList | tr -d '[]"\r' | tr ',' ' '); do
  adb shell settings delete global "CustomKey_$code" >/dev/null
done
adb shell settings delete global CustomKeyList >/dev/null

# The radio turns the sleep timer off while it plays and saves the old value; put it back if it's still off.
if [ "$(adb shell settings get secure sleep_timeout | tr -d '\r')" = "-1" ]; then
  saved=$(adb shell "run-as tv.afnon.home cat shared_prefs/afnon.xml" 2>/dev/null | sed -n 's/.*name="sleep_saved" value="\([0-9]*\)".*/\1/p')
  adb shell settings put secure sleep_timeout "${saved:-1800000}"
fi

# Uninstall Afnon application
echo "tv.afnon.home: $(adb uninstall tv.afnon.home 2>&1 | tr -d '\r')"

# Return to home screen and prompt user to reboot
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo "Done. Reboot the TV to finish."