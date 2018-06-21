/*
 * ao-net-partial-url - Matches and resolves partial URLs.
 * Copyright (C) 2018  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-net-partial-url.
 *
 * ao-net-partial-url is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-net-partial-url is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-net-partial-url.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.net.partialurl;

import com.aoindustries.math.SafeMath;
import com.aoindustries.net.HostAddress;
import com.aoindustries.net.Path;
import com.aoindustries.net.Port;
import com.aoindustries.util.AoCollections;
import com.aoindustries.validation.ValidationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.ObjectUtils;

/**
 * A {@link PartialURL} that may contains multiple values for each field matched.
 * All fields are optional.
 */
public class MultiPartialURL extends PartialURL {

	/**
	 * Gets a partial URL supporting requests across multiple schemes/hosts/ports/...
	 * 
	 * @param scheme       (Optional) The scheme (http/https/...) to match and/or link to, converted to lower-case.
	 * @param host         (Optional) The IP/host to match and/or link to
	 * @param port         (Optional) The port to match and/or link to
	 * @param contextPath  (Optional) The contextPath to match and/or link to
	 * @param prefix       (Optional) The prefix to match against the path or {@code null} to match all.
	 *                                Must be either {@code null} or always ends in a slash (/).
	 *
	 * @see  #valueOf(java.lang.Iterable)
	 * @see  #valueOf(com.aoindustries.net.Path...)
	 * @see  #MultiPartialURL(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set)
	 */
	public static PartialURL valueOf(
		Iterable<? extends String> schemes,
		Iterable<? extends HostAddress> hosts,
		Iterable<? extends Port> ports,
		Iterable<? extends Path> contextPaths,
		Iterable<? extends Path> prefixes
	) {
		Set<String> schemeSet;
		if(schemes == null) schemeSet = null;
		else {
			Set<String> schemesLower = new LinkedHashSet<String>();
			for(String scheme : schemes) schemesLower.add(scheme.toLowerCase(Locale.ROOT));
			schemeSet = AoCollections.optimalUnmodifiableSet(schemesLower);
			if(schemeSet.isEmpty()) schemeSet = null;
		}
		Set<HostAddress> hostSet;
		if(hosts == null) hostSet = null;
		else {
			hostSet = AoCollections.unmodifiableCopySet(hosts);
			if(hostSet.isEmpty()) hostSet = null;
		}
		Set<Port> portSet;
		if(ports == null) portSet = null;
		else {
			portSet = AoCollections.unmodifiableCopySet(ports);
			if(portSet.isEmpty()) portSet = null;
		}
		Set<Path> contextPathSet;
		if(contextPaths == null) contextPathSet = null;
		else {
			contextPathSet = AoCollections.unmodifiableCopySet(contextPaths);
			if(contextPathSet.isEmpty()) contextPathSet = null;
		}
		Set<Path> prefixSet;
		if(prefixes == null) prefixSet = null;
		else {
			prefixSet = AoCollections.unmodifiableCopySet(prefixes);
			if(prefixSet.isEmpty()) prefixSet = null;
		}
		if(
			(schemeSet == null || schemeSet.size() == 1)
			&& (hostSet == null || hostSet.size() == 1)
			&& (portSet == null || portSet.size() == 1)
			&& (contextPathSet == null || contextPathSet.size() == 1)
			&& (prefixSet == null || prefixSet.size() == 1)
		) {
			return SinglePartialURL.valueOf(
				schemeSet == null ? null : schemeSet.iterator().next(),
				hostSet == null ? null : hostSet.iterator().next(),
				portSet == null ? null : portSet.iterator().next(),
				contextPathSet == null ? null : contextPathSet.iterator().next(),
				prefixSet == null ? null : prefixSet.iterator().next()
			);
		} else {
			return new MultiPartialURL(schemeSet, hostSet, portSet, contextPathSet, prefixSet);
		}
	}

	/**
	 * Gets a partial URL always within the current request.
	 *
	 * @param prefix       (Optional) The prefix to match against the path or {@code null} to match all.
	 *                                Must be either {@code null} or always ends in a slash (/).
	 *
	 * @see  #valueOf(java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable)
	 */
	public static PartialURL valueOf(Iterable<? extends Path> prefixes) {
		return valueOf(null, null, null, null, prefixes);
	}

	/**
	 * Gets a partial URL always within the current request.
	 *
	 * @param prefix       (Optional) The prefix to match against the path or {@code null} to match all.
	 *                                Must be either {@code null} or always ends in a slash (/).
	 *
	 * @see  #valueOf(java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable)
	 */
	public static PartialURL valueOf(Path ... prefixes) {
		return valueOf(Arrays.asList(prefixes));
	}

	private final Set<String> schemes;
	private final Set<HostAddress> hosts;
	private final Set<Port> ports;
	private final Set<Path> contextPaths;
	private final Set<Path> prefixes;

	private final SinglePartialURL primary;

	/**
	 * @see  #valueOf(java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable, java.lang.Iterable)
	 */
	private MultiPartialURL(Set<String> schemes, Set<HostAddress> hosts, Set<Port> ports, Set<Path> contextPaths, Set<Path> prefixes) {
		this.schemes = schemes;
		this.hosts = hosts;
		this.ports = ports;
		if(contextPaths != null) {
			for(Path contextPath : contextPaths) {
				if(contextPath != Path.ROOT) {
					String contextPathStr = contextPath.toString();
					if(contextPathStr.endsWith(Path.SEPARATOR_STRING)) {
						throw new IllegalArgumentException("Non-root context path may not end in slash (" + Path.SEPARATOR_CHAR + "): " + contextPath);
					}
				}
			}
		}
		this.contextPaths = contextPaths;
		if(prefixes != null) {
			for(Path prefix : prefixes) {
				if(!prefix.toString().endsWith(Path.SEPARATOR_STRING)) {
					throw new IllegalArgumentException("Prefix does not end in slash (" + Path.SEPARATOR_CHAR + "): " + prefix);
				}
			}
		}
		this.prefixes = prefixes;
		// Generate primary now
		primary = SinglePartialURL.valueOf(
			schemes == null ? null : schemes.iterator().next(),
			hosts == null ? null : hosts.iterator().next(),
			ports == null ? null : ports.iterator().next(),
			contextPaths == null ? null : contextPaths.iterator().next(),
			prefixes == null ? null : prefixes.iterator().next()
		);
	}

	@Override
	public String toString() {
		StringBuilder toString = new StringBuilder();
		// scheme(s)
		if(schemes != null) {
			if(schemes.size() == 1) {
				toString.append(schemes.iterator().next());
			} else {
				toString.append('{');
				boolean didOne = false;
				for(String scheme : schemes) {
					if(didOne) toString.append(',');
					else didOne = true;
					toString.append(scheme);
				}
				toString.append('}');
			}
			toString.append(':');
		}
		toString.append("//");
		if(hosts == null) {
			toString.append(WILDCARD_CHAR);
		} else if(hosts.size() == 1) {
			toString.append(hosts.iterator().next().toBracketedString());
		} else {
			toString.append('{');
			boolean didOne = false;
			for(HostAddress host : hosts) {
				if(didOne) toString.append(',');
				else didOne = true;
				toString.append(host.toBracketedString());
			}
			toString.append('}');
		}
		if(ports == null) {
			toString.append(':').append(WILDCARD_CHAR);
		} else if(ports.size() == 1) {
			// Check if there is only one scheme, hide port when is default
			String scheme;
			if(schemes == null || schemes.size() > 1) {
				scheme = null;
			} else {
				assert schemes.size() == 1;
				scheme = schemes.iterator().next();
			}
			int portNum = ports.iterator().next().getPort();
			if(
				!(HTTP.equals(scheme) && portNum == 80)
				&& !(HTTPS.equals(scheme) && portNum == 443)
			) {
				toString.append(':').append(portNum);
			}
		} else {
			toString.append(":{");
			boolean didOne = false;
			for(Port port : ports) {
				if(didOne) toString.append(',');
				else didOne = true;
				toString.append(port.getPort());
			}
			toString.append('}');
		}
		if(contextPaths == null) {
			toString.append(NULL_CONTEXT_PATH);
		} else if(contextPaths.size() == 1) {
			Path contextPath = contextPaths.iterator().next();
			if(contextPath != Path.ROOT) toString.append(contextPath);
		} else {
			toString.append('{');
			boolean didOne = false;
			for(Path contextPath : contextPaths) {
				if(didOne) toString.append(',');
				else didOne = true;
				if(contextPath != Path.ROOT) toString.append(contextPath);
			}
			toString.append('}');
		}
		if(prefixes == null) {
			toString.append(NULL_PREFIX);
		} else if(prefixes.size() == 1) {
			toString.append(prefixes.iterator().next());
		} else {
			toString.append('{');
			boolean didOne = false;
			for(Path prefix : prefixes) {
				if(didOne) toString.append(',');
				else didOne = true;
				toString.append(prefix);
			}
			toString.append('}');
		}
		return toString.toString();
	}

	@Override
	@SuppressWarnings("deprecation") // Java 1.7: No longer suppress
	public boolean equals(Object obj) {
		if(!(obj instanceof MultiPartialURL)) return false;
		MultiPartialURL other = (MultiPartialURL)obj;
		return
			ObjectUtils.equals(schemes, other.schemes)
			&& ObjectUtils.equals(hosts, other.hosts)
			&& ObjectUtils.equals(ports, other.ports)
			&& ObjectUtils.equals(contextPaths, other.contextPaths)
			&& ObjectUtils.equals(prefixes, other.prefixes);
	}

	@Override
	@SuppressWarnings("deprecation") // Java 1.7: No longer suppress
	public int hashCode() {
		return ObjectUtils.hashCodeMulti(
			schemes,
			hosts,
			ports,
			contextPaths,
			prefixes
		);
	}

	/**
	 * Sequential implementation of {@link #matches(com.aoindustries.net.partialurl.FieldSource)}, only
	 * used for assertions.  This verifies the matching defined in {@link PartialURL#matches(com.aoindustries.net.partialurl.FieldSource)}.
	 *
	 * @see  #matches(com.aoindustries.net.partialurl.FieldSource)
	 * @see  PartialURL#matches(com.aoindustries.net.partialurl.FieldSource)
	 * @see  #getCombinations()
	 */
	private SinglePartialURL matchesSequential(FieldSource fieldSource) throws MalformedURLException {
		for(SinglePartialURL single : getCombinations()) {
			SinglePartialURL match = single.matches(fieldSource);
			if(match != null) {
				assert match == single;
				return single;
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("deprecation") // Java 1.7: Do not suppress
	public SinglePartialURL matches(FieldSource fieldSource) throws MalformedURLException {
		SinglePartialURL match;
		String scheme = null;
		if(schemes != null && !schemes.contains(scheme = fieldSource.getScheme().toLowerCase(Locale.ROOT))) {
			match = null;
		} else {
			HostAddress host = null;
			if(hosts != null && !hosts.contains(host = fieldSource.getHost())) {
				match = null;
			} else {
				Port port = null;
				if(ports != null && !ports.contains(port = fieldSource.getPort())) {
					match = null;
				} else {
					Path contextPath = null;
					if(contextPaths != null && !contextPaths.contains(contextPath = fieldSource.getContextPath())) {
						match = null;
					} else {
						if(prefixes == null) {
							match = SinglePartialURL.valueOf(scheme, host, port, contextPath, null);
						} else {
							Path path = fieldSource.getPath();
							if(path == null) {
								match = null;
							} else {
								String pathStr = path.toString();
								// TODO: Max path length like in index?
								// TODO: Max slashes like index?
								int lastSlash = pathStr.lastIndexOf(Path.SEPARATOR_CHAR);
								assert lastSlash != -1;
								match = null;
								do {
									Path prefix;
									try {
										// TODO: Make a "prefix" method on Path object itself?  Which would not throw ValidationException (but IllegalArgumentException when len is 0)?
										prefix = Path.valueOf(pathStr.substring(0, lastSlash + 1));
									} catch(ValidationException e) {
										AssertionError ae = new AssertionError("Prefix of a valid Path is also a valid Path");
										ae.initCause(e);
										throw ae;
									}
									if(prefixes.contains(prefix)) {
										match = SinglePartialURL.valueOf(scheme, host, port, contextPath, prefix);
										break;
									}
									lastSlash = pathStr.lastIndexOf(Path.SEPARATOR_CHAR, lastSlash - 1);
								} while(lastSlash != -1);
							}
						}
					}
				}
			}
		}
		assert ObjectUtils.equals(match, matchesSequential(fieldSource)) : "matches inconsistent with matchesSequential";
		return match;
	}

	@Override
	public boolean isComplete() {
		return
			schemes != null
			&& hosts != null
			&& ports != null
			&& contextPaths != null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec Uses the first value from each set.
	 */
	@Override
	public SinglePartialURL getPrimary() {
		return primary;
	}

	/**
	 * Iterates a single null value.
	 */
	private static final Iterable<?> NULL_ITERABLE = Collections.singleton(null);
	@SuppressWarnings("unchecked")
	private static <T> Iterable<T> nullIterable() {
		return (Iterable<T>)NULL_ITERABLE;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Sets are iterated in the following order:
	 * </p>
	 * <ol>
	 * <li>{@link #getHosts()}</li>
	 * <li>{@link #getContextPaths()}</li>
	 * <li>{@link #getPrefixes() (sorted by deepest first for consistency with {@link SinglePartialURL#compareTo(com.aoindustries.net.partialurl.SinglePartialURL)})</li>
	 * <li>{@link #getPorts()}</li>
	 * <li>{@link #getSchemes()}</li>
	 * </ol>
	 * <p>
	 * TODO: A more space-efficient implementation could generate these on-the-fly.
	 * Or should we just return the generated {@link Set}?
	 * Will depend on how his is used.
	 * </p>
	 */
	// TODO: First result might not be primary due to prefix reordering
	//       Make a test that catches this issue, then re-order prefixes on MultiPartialURL creation.
	//       Document that primary URL is the deepest path, or result of ordering.
	//       Alternatively, no longer make it such that primary is the first returned be getCombinations.
	@Override
	public Iterable<SinglePartialURL> getCombinations() {
		long combinations = SafeMath.multiply(
			(hosts == null ? 1L : (long)hosts.size()),
			(contextPaths == null ? 1L : (long)contextPaths.size()),
			(prefixes == null ? 1L : (long)prefixes.size()),
			(ports == null ? 1L : (long)ports.size()),
			(schemes == null ? 1L : (long)schemes.size())
		);
		if(combinations > Integer.MAX_VALUE) throw new IllegalStateException("Too many combinations: " + combinations);
		long capacity = combinations*4/3+1;
		if(capacity > Integer.MAX_VALUE) throw new IllegalStateException("Too many combinations: " + combinations);
		Set<SinglePartialURL> results = new LinkedHashSet<SinglePartialURL>((int)capacity);

		Iterable<HostAddress> hostIter;
		if(hosts == null) hostIter = nullIterable();
		else hostIter = hosts;

		Iterable<Path> contextPathIter;
		if(contextPaths == null) contextPathIter = nullIterable();
		else contextPathIter = contextPaths;

		Iterable<Path> prefixIter;
		if(prefixes == null) prefixIter = nullIterable();
		else {
			SortedSet<Path> sortedPrefixes = new TreeSet<Path>(SinglePartialURL.prefixComparator);
			sortedPrefixes.addAll(prefixes);
			prefixIter = sortedPrefixes;
		}

		Iterable<Port> portIter;
		if(ports == null) portIter = nullIterable();
		else portIter = ports;

		Iterable<String> schemesIter;
		if(schemes == null) schemesIter = nullIterable();
		else schemesIter = schemes;

		for(HostAddress host : hostIter) {
			for(Path contextPath : contextPathIter) {
				for(Path prefix : prefixIter) {
					for(Port port : portIter) {
						for(String scheme : schemesIter) {
							SinglePartialURL single = SinglePartialURL.valueOf(scheme, host, port, contextPath, prefix);
							// Use existing primary object for first element in the results.
							if(single.equals(primary)) {
								if(!results.isEmpty()) throw new AssertionError("Primary must be the first element in the results: " + single);
								single = primary;
							}
							if(!results.add(single)) {
								throw new AssertionError("Unexpected duplicate partial URL combination: " + single);
							}
						}
					}
				}
			}
		}
		if(results.size() != combinations) throw new AssertionError("Unexpected number of combinations.  Expected " + combinations + ", got " + results.size());
		return results;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec  This uses the {@link #getPrimary() primary} to create the URL.
	 *
	 * @see  #getPrimary()
	 * @see  SinglePartialURL#toURL(com.aoindustries.net.partialurl.FieldSource)
	 */
	// TODO: Should this use the value matching field source from each set, instead of assuming primary?  Could fall-back to primary when fieldSource not provided.
	// TODO: Should this method only exist on SinglePartialURL?
	@Override
	public URL toURL(FieldSource fieldSource) throws MalformedURLException {
		return getPrimary().toURL(fieldSource);
	}

	/**
	 * Gets the unmodifiable set of lower-case schemes (such as https/http/other) for this partial URL.
	 *
	 * @return  The schemes or {@code null} when {@link FieldSource#getScheme()} should be used.
	 *
	 * @see  #HTTP
	 * @see  #HTTPS
	 *
	 * @see  FieldSource#getScheme()
	 */
	public Set<String> getSchemes() {
		return schemes;
	}

	/**
	 * Gets the unmodifiable set of IP addresses or hostnamees for this partial URL.
	 *
	 * @return  The IP addresses/hostnames or {@code null} when {@link FieldSource#getHost()} should be used.
	 *
	 * @see  FieldSource#getHost()
	 */
	public Set<HostAddress> getHosts() {
		return hosts;
	}

	/**
	 * Gets the unmodifiable set of port numbers for this partial URL.
	 *
	 * @return  The ports or {@code null} when {@link FieldSource#getPort()} should be used.
	 *
	 * @see  FieldSource#getPort()
	 */
	public Set<Port> getPorts() {
		return ports;
	}

	/**
	 * Gets the unmodifiable set of context paths for this partial URL, only ending in a slash (/) when is
	 * {@link Path#ROOT the root context}.
	 *
	 * @return  The context paths or {@code null} when the {@link FieldSource#getContextPath() context path of the field source} should be used.
	 *
	 * @see  FieldSource#getContextPath()
	 */
	public Set<Path> getContextPaths() {
		return contextPaths;
	}

	/**
	 * Gets the unmodifiable set of prefixes of the path for this partial URL, always either {@code null} or ending in a slash (/).
	 * This is matched as a prefix of {@link  FieldSource#getPath()}.
	 *
	 * @see  FieldSource#getPath()
	 * @see  Path#SEPARATOR_CHAR
	 */
	public Set<Path> getPrefixes() {
		return prefixes;
	}
}