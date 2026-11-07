#!/usr/bin/env node
"use strict";
// Restricted VM: AI code receives only api.scene and safe data primitives.
const fs = require("fs");
const vm = require("vm");
const input = process.argv[2], output = process.argv[3];
if (!input || !output) throw new Error("Usage: node animation_runtime.js project.json normalized.json");
const project = JSON.parse(fs.readFileSync(input, "utf8"));
const source = String(project.animation_code || "").trim();
const plan = [];
const allowedMotion = new Set(["float", "bounce", "slide", "pulse", "still"]);
const api = Object.freeze({
  scene(value) {
    if (plan.length >= 60) throw new Error("Maximum 60 generated scenes allowed");
    if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("api.scene expects an object");
    const title = String(value.title || ("Scene " + (plan.length + 1))).slice(0, 80);
    const description = String(value.description || "").slice(0, 500);
    const rawColor = String(value.background || "#142b2a");
    const background = /^#[0-9a-fA-F]{6}$/.test(rawColor) ? rawColor : "#142b2a";
    const requestedDuration = Number(value.duration);
    const duration = Number.isFinite(requestedDuration) ? Math.max(1, Math.min(600, requestedDuration)) : 10;
    const motion = allowedMotion.has(value.motion) ? value.motion : "float";
    plan.push({title, description, background, duration, motion});
  }
});
Object.setPrototypeOf(api.scene, null);
Object.freeze(api.scene);
Object.setPrototypeOf(api, null);
Object.freeze(api);
if (source) {
  if (source.length > 24000) throw new Error("Generated animation code exceeds 24 KB");
  const context = vm.createContext({api}, {codeGeneration:{strings:false,wasm:false}});
  new vm.Script('"use strict"; (' + source + ')(api);', {filename:"ai-animation.js"}).runInContext(context, {timeout:1500});
}
if (!plan.length) {
  for (const scene of (project.scenes || [])) plan.push({title:String(scene).slice(0,80),description:String(scene).slice(0,500),background:"#142b2a",duration:10,motion:"float"});
}
if (!plan.length) throw new Error("No scenes were produced by the AI animation script");
project.animation_plan = plan;
fs.writeFileSync(output, JSON.stringify(project));
console.log("Sandbox accepted " + plan.length + " scenes. Code bytes: " + Buffer.byteLength(source));
