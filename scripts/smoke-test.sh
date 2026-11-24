#!/usr/bin/env bash
# Smoke test of a running Book Store (ADR-0022): walks the main customer and administrator flows
# over HTTP, the way the pages and API clients use them. CI runs it against Payara Micro on
# PostgreSQL 17; locally it works against any running server.
#
#   ./scripts/smoke-test.sh                       against http://localhost:8080
#   BASE_URL=http://localhost:8081 ./scripts/smoke-test.sh
#   WAIT_SECONDS=360 ./scripts/smoke-test.sh     wait longer for the server to start (default 180)
#
# It registers a new customer and places an order, so run it on a test server, not on real data.
# Needs only curl, grep and sed: the server writes compact JSON with sorted keys, which is read with
# grep here instead of a JSON tool.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
API="$BASE/api/v1"
ADMIN="admin@bookstore.local:kitap2026"
JSON="Content-Type: application/json"
WEB="X-Bookstore-Client: web"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
JAR="$WORK/cookies"
step=0

pass() { step=$((step + 1)); printf '  ok %2d  %s\n' "$step" "$1"; }
fail() { printf '  FAILED  %s\n' "$1" >&2; [[ -s "$WORK/body" ]] && { printf '  answer: ' >&2; head -c 600 "$WORK/body" >&2; echo >&2; }; exit 1; }

# call METHOD PATH [curl options...] -> status in $status, body in $WORK/body, headers in $WORK/headers
call() {
    local method="$1" path="$2"; shift 2
    status=$(curl -s -o "$WORK/body" -D "$WORK/headers" -w '%{http_code}' -X "$method" "$@" "$API$path")
}
expect() { [[ "$status" == "$1" ]] || fail "$2 (expected HTTP $1, got $status)"; pass "$2"; }
field() { grep -o "\"$1\":[^,}]*" "$WORK/body" | head -n 1 | cut -d: -f2- | tr -d '"'; }
header() { grep -i "^$1:" "$WORK/headers" | head -n 1 | cut -d' ' -f2- | tr -d '\r'; }

echo "Smoke test of $BASE"

for _ in $(seq 1 $(( ${WAIT_SECONDS:-180} / 2 ))); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/health/ready")" == "200" ]] && break
    sleep 2
done
status=$(curl -s -o "$WORK/body" -w '%{http_code}' "$BASE/health/ready")
expect 200 "the server is ready: database reachable, migrations done"

call GET "/books?size=50"
expect 200 "the catalog lists books"
[[ "$(field totalElements)" -ge 1 ]] || fail "the catalog has books"
book=$(grep -o '"id":[0-9]*,"inStock":true' "$WORK/body" | head -n 1 | sed -E 's/"id":([0-9]+),.*/\1/')
[[ -n "$book" ]] || fail "a book in stock is listed"
[[ -n "$(header X-Correlation-Id)" ]] || fail "answers carry a correlation id"
pass "answers carry a correlation id"

call GET "/books?q=dune"
expect 200 "the text search answers"

email="smoke-$(date +%s)-$RANDOM@example.com"
call POST "/customers" -H "$JSON" -d "{\"email\":\"$email\",\"firstName\":\"Duman\",\"lastName\":\"Testi\",\"password\":\"smoke2026\"}"
expect 201 "a visitor registers"

call POST "/cart/items" -c "$JAR" -b "$JAR" -H "$JSON" -H "$WEB" -d "{\"bookId\":$book,\"quantity\":1}"
expect 200 "a visitor fills a cart kept in the session"

call POST "/session" -c "$JAR" -b "$JAR" -H "$JSON" -H "$WEB" -d "{\"email\":\"$email\",\"password\":\"smoke2026\"}"
expect 200 "the customer signs in with the session cookie"

call GET "/cart" -b "$JAR" -H "$WEB"
expect 200 "the stored cart answers"
[[ "$(field totalQuantity)" == "1" ]] || fail "the visitor's cart joined the customer's cart"
pass "the visitor's cart joined the customer's cart"

call POST "/cart/items" -b "$JAR" -H "$JSON" -d "{\"bookId\":$book,\"quantity\":1}"
expect 403 "a cookie request without the client header is refused"

call POST "/me/addresses" -b "$JAR" -H "$JSON" -H "$WEB" -d '{"label":"Ev","recipient":"Duman Testi","phone":"05550000000","line":"Test Sokak 1","district":"Kadıköy","city":"İstanbul","postalCode":"34710","makeDefault":true}'
expect 201 "the customer saves an address"
address=$(header Location | sed 's|.*/||')

key="smoke-$RANDOM-$RANDOM"
order_body="{\"addressId\":$address,\"paymentMethod\":\"CREDIT_CARD\"}"
call POST "/orders" -b "$JAR" -H "$JSON" -H "$WEB" -H "Idempotency-Key: $key" -d "$order_body"
expect 201 "the customer places an order paid by card"
order=$(field orderNumber)
[[ "$(field status)" == "PAID" ]] || fail "a card order is paid"
call POST "/orders" -b "$JAR" -H "$JSON" -H "$WEB" -H "Idempotency-Key: $key" -d "$order_body"
expect 201 "sending the same order again is answered"
[[ "$(field orderNumber)" == "$order" ]] || fail "the repeat gets the same order, not a second one"
pass "the repeat gets the same order, not a second one"

call PUT "/admin/orders/$order/status" -b "$JAR" -H "$JSON" -H "$WEB" -d '{"status":"SHIPPED"}'
expect 403 "a customer cannot move an order"
call PUT "/admin/orders/$order/status" -u "$ADMIN" -H "$JSON" -d '{"status":"SHIPPED"}'
expect 200 "the administrator ships the order"
call PUT "/admin/orders/$order/status" -u "$ADMIN" -H "$JSON" -d '{"status":"DELIVERED"}'
expect 200 "the administrator marks it delivered"

call GET "/books/$book/reviews/eligibility" -b "$JAR"
[[ "$status" == "200" && "$(field canReview)" == "true" ]] || fail "the customer may review a delivered book"
pass "the customer may review a delivered book"
call POST "/books/$book/reviews" -b "$JAR" -H "$JSON" -H "$WEB" -d '{"stars":5,"comment":"Duman testi"}'
expect 201 "the customer writes a review"

call GET "/admin/books/$book" -u "$ADMIN"
expect 200 "the administrator reads the book"
tag=$(header ETag)
stock=$(field stock)
call PUT "/admin/books/$book/stock" -u "$ADMIN" -H "$JSON" -H "If-Match: $tag" -d "{\"stock\":$stock}"
expect 200 "a change with the current entity tag is taken"
call PUT "/admin/books/$book/stock" -u "$ADMIN" -H "$JSON" -H "If-Match: $tag" -d "{\"stock\":$stock}"
expect 412 "a change with an outdated entity tag is refused"

call DELETE "/session" -b "$JAR" -c "$JAR" -H "$WEB"
expect 204 "the customer signs out"
call GET "/session" -b "$JAR"
expect 401 "the session has ended"

status=$(curl -s -o "$WORK/body" -w '%{http_code}' "$BASE/")
expect 200 "the shop's home page is served"

echo "All $step checks passed."
