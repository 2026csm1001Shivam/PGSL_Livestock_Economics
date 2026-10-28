# GADVASU Livestock Economics: Firestore database schema

This follows the schema on slide 9 of the project presentation, extended with the
fields the app now has (herd size, cow and buffalo milk prices, the feed list, pond area).

> Used by the app: `data/firebase/CloudSync.kt` writes and reads these documents.

## Structure

```
users/{uid}                                  one document per farmer (uid = Firebase Auth user ID)
 ├── dailyRecords/{enterprise}_{date}        Livestock and Layer, one per enterprise per day
 │     e.g. livestock_2026-10-07, layer_2026-10-07
 └── batches/{batchId}                       Broiler, Goatery, Fishery
       └── expenses/{expenseId}              expense entries added while the batch is running
```

"Contains" means sub-collection: each farmer has many daily records and many batches;
each batch has many expense entries. Because a daily record's ID is `enterprise_date`,
saving the same day twice updates the existing record instead of creating a duplicate.

## Collections and fields

### `users/{uid}`
| Field | Type | Notes |
|---|---|---|
| name | string | optional |
| phone | string or null | 10-digit mobile, if the farmer logged in with mobile |
| email | string or null | if the farmer logged in with email |
| language | string | `en`, `hi` or `pa` |
| role | string | always `farmer` (rules block anything else) |
| createdAt | timestamp | |
| updatedAt | timestamp | server time |

### `users/{uid}/dailyRecords/{enterprise}_{date}`
| Field | Type | Notes |
|---|---|---|
| enterprise | string | `livestock` or `layer` |
| date | string | `yyyy-MM-dd`; stored as text so it sorts and filters correctly |
| production | number | litres of milk (livestock) or number of eggs (layer) |
| income | map | livestock: `cowMilk`, `buffaloMilk`, `byproduct`, `other`; layer: `egg`, `other` |
| expenses | map | livestock: `feed`, `electricity`, `labour`, `vet`, `water`, `other`; layer: `feed`, `electricity`, `labour`, `medicine`, `other` |
| totalIncome | number | sum of `income`, stored so reports don't recompute |
| totalExpense | number | sum of `expenses` |
| profit | number | `totalIncome - totalExpense` |
| note | string | |
| livestock | map | **livestock only** (new): see below |
| createdAt / updatedAt | timestamp | |

`livestock` map (new fields from the updated Livestock screen):
```
livestock: {
  cows: 10,
  buffaloes: 5,
  cowMilk:     { litres: 15, pricePerLitre: 45 },
  buffaloMilk: { litres: 10, pricePerLitre: 57.5 },
  feeds: [ { name: "Silage", price: 270 }, { name: "Cattle feed", price: 200 } ]
}
```
`income.cowMilk` = 15 × 45, `income.buffaloMilk` = 10 × 57.5, and `expenses.feed` = the sum of `feeds`.
Both the detail and the totals are stored: the detail for editing, the totals for reports.

### `users/{uid}/batches/{batchId}`
| Field | Type | Notes |
|---|---|---|
| enterprise | string | `broiler`, `goatery` or `fishery` |
| name | string | e.g. "Batch A" |
| startDate | string | `yyyy-MM-dd` |
| count | integer | birds, goats or fish seed |
| purchaseCost | number | slide 9 calls this `cost` |
| pondArea | number | acres, fishery only (new) |
| note | string | |
| status | string | `running` or `completed` |
| saleDate | string or null | set when completed |
| amountReceived | number | slide 9 calls this `amount` |
| saleNote | string | |
| totalExpense | number | purchase cost + all expense entries |
| profit | number or null | `amountReceived - totalExpense`, null while running |
| createdAt / updatedAt | timestamp | |

### `users/{uid}/batches/{batchId}/expenses/{expenseId}`
| Field | Type | Notes |
|---|---|---|
| date | string | `yyyy-MM-dd` |
| expenses | map | e.g. `{ feed: 2000, labour: 500 }` (slide 9 lists these as separate fields; a map keeps it the same shape as daily records) |
| total | number | sum of `expenses` |
| note | string | |
| createdAt | timestamp | |

## Worked examples (the numbers from slide 3)

**Daily: Livestock, 7 Oct, 25 litres** → `users/{uid}/dailyRecords/livestock_2026-10-07`
```json
{
  "enterprise": "livestock", "date": "2026-10-07", "production": 25,
  "income":   { "cowMilk": 675, "buffaloMilk": 575, "byproduct": 200, "other": 0 },
  "expenses": { "feed": 470, "electricity": 60, "labour": 200, "vet": 0, "water": 20, "other": 30 },
  "totalIncome": 1450, "totalExpense": 780, "profit": 670,
  "livestock": {
    "cows": 10, "buffaloes": 5,
    "cowMilk": { "litres": 15, "pricePerLitre": 45 },
    "buffaloMilk": { "litres": 10, "pricePerLitre": 57.5 },
    "feeds": [ { "name": "Silage", "price": 270 }, { "name": "Cattle feed", "price": 200 } ]
  },
  "note": ""
}
```

**Batch: Goatery, Batch A, 20 goats, 97 days** → `users/{uid}/batches/{batchId}`
```json
{
  "enterprise": "goatery", "name": "Batch A", "startDate": "2026-07-02", "count": 20,
  "purchaseCost": 80000, "pondArea": 0, "status": "completed",
  "saleDate": "2026-10-07", "amountReceived": 168000,
  "totalExpense": 132000, "profit": 36000
}
```
with expense entries under `.../expenses/` adding up to feed ₹28,500, labour ₹18,000 and
vet/electricity/water/other ₹5,500.

## Security (firestore.rules)

- A farmer can only read and write documents under their own `users/{uid}`.
- `role` must be `farmer`, `language` must be `en`/`hi`/`pa`.
- Daily records: `enterprise` must be `livestock`/`layer`, `date` must look like `yyyy-MM-dd`,
  and the document ID must equal `enterprise_date` (so there's one record per day).
- Batches: `enterprise` must be `broiler`/`goatery`/`fishery`, `status` `running`/`completed`, `count` > 0.
