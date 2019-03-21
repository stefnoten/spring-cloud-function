/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.function.stream.function;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = IsolatedMessagePojoStreamingFunctionTests.StreamingFunctionApplication.class)
public class IsolatedMessagePojoStreamingFunctionTests {

	@Autowired
	Processor processor;

	@Autowired
	MessageCollector messageCollector;

	@Test
	public void test() throws Exception {
		processor.input().send(
				MessageBuilder.withPayload(new String("{\"name\":\"foo\"}")).build());
		Message<?> result = messageCollector.forChannel(processor.output()).poll(1000,
				TimeUnit.MILLISECONDS);
		assertThat(result.getPayload().getClass().getName())
				.isEqualTo(Foo.class.getName());
	}

	@SpringBootApplication
	public static class StreamingFunctionApplication {

		@Autowired
		private FunctionRegistry registry;

		@PostConstruct
		public void register() {
			ClassLoader loader = ClassLoaderUtils.createClassLoader();
			Class<?> type = ClassUtils.resolveClassName(Uppercase.class.getName(),
					loader);
			registry.register(
					new FunctionRegistration<Object>(BeanUtils.instantiate(type))
							.name("uppercase"));
		}

	}

	public static class Uppercase implements Function<Message<Foo>, Message<Foo>> {
		@Override
		public Message<Foo> apply(Message<Foo> flux) {
			return MessageBuilder
					.withPayload(new Foo(flux.getPayload().getName().toUpperCase()))
					.build();
		}
	}

	protected static class Foo {
		private String name;

		Foo() {
		}

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
