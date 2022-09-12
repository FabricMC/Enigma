/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public interface Translator {
	@Nullable
	<T extends Translatable> TranslateResult<T> extendedTranslate(@Nullable T translatable);

	@Deprecated
	@Nullable
	default <T extends Translatable> T translate(@Nullable T translatable) {
		TranslateResult<T> res = this.extendedTranslate(translatable);
		return res == null ? null : res.getValue();
	}

	default <T extends Translatable> Collection<T> translate(Collection<T> translatable) {
		return translatable.stream().map(this::translate).toList();
	}

	default <T extends Translatable> Set<T> translate(Set<T> translatable) {
		return translatable.stream().map(this::translate).collect(Collectors.toSet());
	}

	default <T extends Translatable, V> Map<T, V> translateKeys(Map<T, V> translatable) {
		Map<T, V> result = new HashMap<>(translatable.size());

		for (Map.Entry<T, V> entry : translatable.entrySet()) {
			result.put(translate(entry.getKey()), entry.getValue());
		}

		return result;
	}

	default <K extends Translatable, V extends Translatable> Map<K, V> translate(Map<K, V> translatable) {
		Map<K, V> result = new HashMap<>(translatable.size());

		for (Map.Entry<K, V> entry : translatable.entrySet()) {
			result.put(translate(entry.getKey()), translate(entry.getValue()));
		}

		return result;
	}

	default <K extends Translatable, V extends Translatable> Multimap<K, V> translate(Multimap<K, V> translatable) {
		Multimap<K, V> result = HashMultimap.create(translatable.size(), 1);

		for (Map.Entry<K, Collection<V>> entry : translatable.asMap().entrySet()) {
			result.putAll(translate(entry.getKey()), translate(entry.getValue()));
		}

		return result;
	}
}
