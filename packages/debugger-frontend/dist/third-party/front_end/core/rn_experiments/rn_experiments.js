import*as e from"../root/root.js";const t=e.Runtime.RNExperimentName,n={didInitializeExperiments:!1,isReactNativeEntryPoint:!1};class i{name;title;unstable;docLink;feedbackLink;enabledByDefault;constructor(e){this.name=e.name,this.title=e.title,this.unstable=e.unstable,this.docLink=e.docLink,this.feedbackLink=e.feedbackLink,this.enabledByDefault=function(e,t){if(null==e)return()=>t;if("boolean"==typeof e)return()=>e;return e}(e.enabledByDefault,!1)}}const a=new class{#e=new Map;#t=new Set;register(e){if(n.didInitializeExperiments)throw new Error("Experiments must be registered before constructing MainImpl");const{name:t}=e;if(this.#e.has(t))throw new Error(`React Native Experiment ${t} is already registered`);this.#e.set(t,new i(e))}enableExperimentsByDefault(e){if(n.didInitializeExperiments)throw new Error("Experiments must be configured before constructing MainImpl");for(const n of e)if(Object.prototype.hasOwnProperty.call(t,n)){const e=this.#e.get(n);if(!e)throw new Error(`React Native Experiment ${n} is not registered`);e.enabledByDefault=()=>!0}else this.#t.add(n)}copyInto(e,t=""){for(const[i,a]of this.#e)e.register(i,t+a.title,a.unstable,a.docLink,a.feedbackLink),a.enabledByDefault({isReactNativeEntryPoint:n.isReactNativeEntryPoint})&&e.enableExperimentsByDefault([i]);for(const t of this.#t)e.enableExperimentsByDefault([t]);n.didInitializeExperiments=!0}};a.register({name:t.JS_HEAP_PROFILER_ENABLE,title:"Enable Heap Profiler",unstable:!1,enabledByDefault:({isReactNativeEntryPoint:e})=>!e}),a.register({name:t.ENABLE_REACT_DEVTOOLS_PANEL,title:"Enable React DevTools panel",unstable:!0,enabledByDefault:!1}),a.register({name:t.REACT_NATIVE_SPECIFIC_UI,title:"Show React Native-specific UI",unstable:!1,enabledByDefault:({isReactNativeEntryPoint:e})=>e}),a.register({name:t.ENABLE_PERFORMANCE_PANEL,title:"Enable Performance panel",unstable:!0,enabledByDefault:({isReactNativeEntryPoint:e})=>!e});var r=Object.freeze({__proto__:null,RNExperimentName:t,setIsReactNativeEntryPoint:function(e){if(n.didInitializeExperiments)throw new Error("setIsReactNativeEntryPoint must be called before constructing MainImpl");n.isReactNativeEntryPoint=e},Instance:a});export{r as RNExperimentsImpl};
