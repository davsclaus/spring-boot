/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link WebFilterHandler}
 *
 * @author Andy Wilkinson
 */
public class WebFilterHandlerTests {

	private final WebFilterHandler handler = new WebFilterHandler();

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@SuppressWarnings("unchecked")
	@Test
	public void defaultFilterConfiguration() throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory()
						.getMetadataReader(DefaultConfigurationFilter.class.getName()));
		this.handler.handle(scanned, this.registry);
		BeanDefinition filterRegistrationBean = this.registry
				.getBeanDefinition(DefaultConfigurationFilter.class.getName());
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported"), is((Object) false));
		assertThat((EnumSet<DispatcherType>) propertyValues.get("dispatcherTypes"),
				is(EnumSet.of(DispatcherType.REQUEST)));
		assertThat(((Map<String, String>) propertyValues.get("initParameters")).size(),
				is(0));
		assertThat((String[]) propertyValues.get("servletNames"), is(arrayWithSize(0)));
		assertThat((String[]) propertyValues.get("urlPatterns"), is(arrayWithSize(0)));
		assertThat(propertyValues.get("name"),
				is((Object) DefaultConfigurationFilter.class.getName()));
		assertThat(propertyValues.get("filter"), is(equalTo((Object) scanned)));
	}

	@Test
	public void filterWithCustomName() throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory()
						.getMetadataReader(CustomNameFilter.class.getName()));
		this.handler.handle(scanned, this.registry);
		BeanDefinition filterRegistrationBean = this.registry.getBeanDefinition("custom");
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("name"), is((Object) "custom"));
	}

	@Test
	public void asyncSupported() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				AsyncSupportedFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported"), is((Object) true));
	}

	@Test
	public void dispatcherTypes() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				DispatcherTypesFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("dispatcherTypes"),
				is((Object) EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE,
						DispatcherType.REQUEST)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void initParameters() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				InitParametersFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((Map<String, String>) propertyValues.get("initParameters"),
				hasEntry("a", "alpha"));
		assertThat((Map<String, String>) propertyValues.get("initParameters"),
				hasEntry("b", "bravo"));
	}

	@Test
	public void servletNames() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				ServletNamesFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("servletNames"),
				is(arrayContaining("alpha", "bravo")));
	}

	@Test
	public void urlPatterns() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				UrlPatternsFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlPatterns"),
				is(arrayContaining("alpha", "bravo")));
	}

	@Test
	public void urlPatternsFromValue() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(
				UrlPatternsFromValueFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlPatterns"),
				is(arrayContaining("alpha", "bravo")));
	}

	@Test
	public void urlPatternsDeclaredTwice() throws IOException {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"The urlPatterns and value attributes are mututally " + "exclusive");
		getBeanDefinition(UrlPatternsDeclaredTwiceFilter.class);
	}

	BeanDefinition getBeanDefinition(Class<?> filterClass) throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory()
						.getMetadataReader(filterClass.getName()));
		this.handler.handle(scanned, this.registry);
		return this.registry.getBeanDefinition(filterClass.getName());
	}

	@WebFilter
	class DefaultConfigurationFilter extends BaseFilter {

	}

	@WebFilter(asyncSupported = true)
	class AsyncSupportedFilter extends BaseFilter {

	}

	@WebFilter(dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD,
			DispatcherType.INCLUDE })
	class DispatcherTypesFilter extends BaseFilter {

	}

	@WebFilter(initParams = { @WebInitParam(name = "a", value = "alpha"),
			@WebInitParam(name = "b", value = "bravo") })
	class InitParametersFilter extends BaseFilter {

	}

	@WebFilter(servletNames = { "alpha", "bravo" })
	class ServletNamesFilter extends BaseFilter {

	}

	@WebFilter(urlPatterns = { "alpha", "bravo" })
	class UrlPatternsFilter extends BaseFilter {

	}

	@WebFilter({ "alpha", "bravo" })
	class UrlPatternsFromValueFilter extends BaseFilter {

	}

	@WebFilter(value = { "alpha", "bravo" }, urlPatterns = { "alpha", "bravo" })
	class UrlPatternsDeclaredTwiceFilter extends BaseFilter {

	}

	@WebFilter(filterName = "custom")
	class CustomNameFilter extends BaseFilter {

	}

	class BaseFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
		}

		@Override
		public void destroy() {

		}

	}

}