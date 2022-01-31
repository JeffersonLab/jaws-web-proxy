import Dexie from './dexie-3.0.3.js';
import {AlarmCategory, AlarmClass, AlarmInstance, AlarmLocation, EffectiveRegistration, KafkaLogPosition} from "./entities.js";

const db = new Dexie("jaws");

db.version(1).stores({
    categories: "name",
    classes: "name",
    effectives: "name",
    instances: "name",
    locations: "name",
    positions: "name"
});

db.categories.mapToClass(AlarmCategory);
db.classes.mapToClass(AlarmClass);
db.effectives.mapToClass(EffectiveRegistration);
db.instances.mapToClass(AlarmInstance);
db.locations.mapToClass(AlarmLocation);
db.positions.mapToClass(KafkaLogPosition);

export default db;