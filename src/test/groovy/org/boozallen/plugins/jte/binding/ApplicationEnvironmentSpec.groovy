/*
   Copyright 2018 Booz Allen Hamilton
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.boozallen.plugins.jte.binding

import spock.lang.* 
import org.junit.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigException

class ApplicationEnvironmentSpec extends Specification{

    TemplateBinding binding = new TemplateBinding() 
    CpsScript script = GroovyMock(CpsScript)

    def setup(){
        _ * script.getBinding() >> {
            return binding 
        }
    }

    void injectEnvironments(Map env_config){
        TemplateConfigObject config = new TemplateConfigObject(config: [
            application_environments: env_config
        ])
        ApplicationEnvironment.Injector.doInject(config, script)
    }

    def "Injector populates binding"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
        then: 
            assert binding.hasVariable(envName) 
            assert binding.getVariable(envName) instanceof ApplicationEnvironment
    }

    def "default short_name is environment key"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.short_name == envName
    }

    def "set short_name"(){
        when:
            def envName = "dev"
            injectEnvironments([
                "${envName}": [
                    short_name: "develop"
                ]
            ])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.short_name == "develop"
    }

    def "default long_name is environment key"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.long_name == envName
    }

    def "set long_name"(){
        when:
            def envName = "dev"
            injectEnvironments([
                "${envName}": [
                    long_name: "develop"
                ]
            ])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.long_name == "develop"
    }

    def "can set arbitrary additional key"(){
        when: 
            def envName = "dev" 
            injectEnvironments([
                "${envName}": [
                    random: 11
                ]
            ])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.random == 11 
    }

    def "missing property returns null"(){
        when: 
            def envName = "dev" 
            injectEnvironments(["${envName}": [:]])
            ApplicationEnvironment env = binding.getVariable(envName)
        then: 
            assert env.random == null 
    } 

    def "application environments are immutable"(){
        when: 
            def envName = "dev" 
            injectEnvironments(["${envName}": [:]])
            ApplicationEnvironment env = binding.getVariable(envName)
            env.short_name = "something else" 
        then: 
            thrown(TemplateConfigException)
    }

    def "can set multiple environments"(){
        when: 
            injectEnvironments([
                dev: [:],
                test: [:]
            ])
        then: 
            assert binding.hasVariable("dev")
            assert binding.hasVariable("test")
    }

    def "fail on override during initialization"(){
        when: 
            injectEnvironments([dev: [:]])
            binding.setVariable("dev", "whatever")
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "Application Environment dev already defined." 
    }

    def "fail on override post initialization"(){
        when: 
            injectEnvironments([dev: [:]])
            binding.lock()
            binding.setVariable("dev", "whatever")
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "Variable dev is reserved as an Application Environment." 
    }
}